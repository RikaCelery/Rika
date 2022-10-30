package org.celery.command.common.hutao

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.getAvatar
import org.celery.utils.number.randomFloat
import org.celery.utils.number.randomInt
import org.celery.utils.strings.placeholder
import java.io.File

/**
 * 胡桃的kt实现
 * 仅可用于群聊
 */
object Hutao : Command("胡桃") {
    private val functions = mutableListOf<Function>()

    /**
     * 一个子功能
     *
     * 功能：对于匹配某一个正则的消息
     *
     * 回复某些消息
     *
     * 对变量进行某些操作
     */
    interface Function {
        val regex: Regex
        suspend fun Group.perform(result: MatchResult, event: GroupMessageEvent)
    }

    sealed interface HMessage {
        class Image(
            val path: String,
        ) : HMessage

        class Plain(
            val content: String,
        ) : HMessage

        companion object {
            fun of(content: String): List<HMessage> =
                content.replace("\\n","\n").lines().map(String::toHMesssage)

        }
    }

    class SimpleReply(
        override val regex: Regex,
        private val replys: List<HMessage>,
    ) : Function {
        override suspend fun Group.perform(result: MatchResult, event: GroupMessageEvent) {
            buildMessageChain {
                add("胡桃对你说:\n")
                replys.forEach {
                    when (it) {
                        is HMessage.Image -> {
                            when (it.path) {
                                "sender" -> add(event.sender.getAvatar(event.group))
                                "group" -> add(event.group.getAvatar(event.group))
                                else -> add(File(it.path).uploadAsImage(this@perform))
                            }
                        }
                        is HMessage.Plain -> add(
                            it.content.placeholder(
                                *result.groupValues.mapIndexed { index, s ->
                                "\\$index" to s }.toTypedArray(),
                                "sender_id" to event.sender.id,
                                "sender_name" to event.sender.nameCardOrNick,
                                "group_id" to event.group.id,
                                "group_name" to event.group.name,

                                )
                        )
                    }
                }
            }.sendTo(this)
        }
    }

    class RandomReply(
        override val regex: Regex,
        val probability: Float,
        vararg val replys: Pair<IntRange, List<HMessage>>,
    ) : Function {
        val sum: Int = replys.sumOf { it.first.last+1 - it.first.first }

        init {
            replys.sortByDescending { it.first.first }
        }

        override suspend fun Group.perform(result: MatchResult, event: GroupMessageEvent) {
            buildMessageChain {
                add("胡桃对你说:\n")
                val randomInt = randomInt(0, sum)
                replys.find { randomInt in it.first }!!.second.forEach {
                    when (it) {
                        is HMessage.Image -> {
                            when (it.path) {
                                "sender" -> add(event.sender.getAvatar(event.group))
                                "group" -> add(event.group.getAvatar(event.group))
                                else -> add(File(it.path).uploadAsImage(this@perform))
                            }
                        }
                        is HMessage.Plain -> add(
                            it.content.placeholder(
                                *result.groupValues.mapIndexed { index, s ->
                                    "\\$index" to s }.toTypedArray(),
                                "sender_id" to event.sender.id,
                                "sender_name" to event.sender.nameCardOrNick,
                                "group_id" to event.group.id,
                                "group_name" to event.group.name,
                                )
                        )
                    }
                }
            }.sendTo(this)
        }
    }

    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult) {
        with(eventMatchResult.getdata<Function>()) {
            group.perform(eventMatchResult.getResult(), this@handle)
        }
    }


    @Trigger("handle")
    fun GroupMessageEvent.match(): EventMatchResult? {
        for (func in functions) {
            val result = func.regex.find(message.serializeToMiraiCode())
            if (result != null) {
                return EventMatchResult(result, data = func)
            }

        }
        return null
    }

    init {
        try {
            getOrCreateDataFile("胡桃.txt").readText().split("[func_start]").map(String::trimIndent)
                .filterNot(String::isBlank).map(String::lines).forEach {

                    val regex = it.first()
                    if (it[1] == "RandomReply") {
                        logger.debug("------------------")
                        var sum = 0
                        logger.debug("probility: ${it[2]}")
                        val map = it.drop(3).filterNot(String::isBlank).map { s ->
                            val start = sum
                            if (s.first().isDigit().not()) {
                                sum += 1
                                Triple(1, start until sum, s)
                                return@map start until sum to HMessage.of(s)
                            }
                            val weight = s.substringBefore('|').toInt()
                            val content = (s.substringAfter('|'))
                            sum += weight
                            Triple(weight, start until sum, content)
                            start until sum to HMessage.of(content)
                        }
                        functions.add(
                            RandomReply(
                                regex.reg(), it[2].toFloat(), *map.toTypedArray()
                            )
                        )
                    } else {
                        val replys = it.drop(1)
                        functions.add(SimpleReply(
                            regex.reg(),
                            replys.map { it.toHMesssage() }
                        ))
                        logger.verbose("regex = ${regex}")
                        logger.verbose("replys = ${replys}")
                    }
                }
            logger.info("胡桃 loaded f -> ${functions.size}")
        } catch (e: Exception) {
            logger.error(e)
        }
    }

}

private fun String.reg(): Regex {
    return this.toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
}

private fun <T> Array<T>.select(): Hutao.HMessage {
    val rand = randomFloat()


    TODO("Not yet implemented")
}

private fun String.toHMesssage(): Hutao.HMessage {
    if (this.removeSurrounding("±").startsWith("img="))
        return Hutao.HMessage.Image((this.removeSurrounding("±").substringAfter("img=")))
    return Hutao.HMessage.Plain(this)
}
