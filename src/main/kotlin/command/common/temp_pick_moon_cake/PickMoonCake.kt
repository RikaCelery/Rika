package org.celery.command.common.temp_pick_moon_cake

import events.ExecutionResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import java.util.concurrent.atomic.AtomicBoolean

object PickMoonCake : RegexCommand(
    "月饼", "^拿月饼\\s*(.*)".toRegex(),
    secondaryRegexs = arrayOf(
        "^查月饼".toRegex()
    )
) {
    private val picking = AtomicBoolean(false)
    override var defaultCoolDown: Long
        get() = 1000
        set(value) {}
    override var defaultCountLimit: Int
        get() = 200
        set(value) {}
    private val moonCakes: MutableList<Cake>
        get() {
            val list = try {
                defaultJson.decodeFromString(getOrCreateDataFile("cakes.json").readText())
            } catch (e: Exception) {
                mutableListOf<Cake>()
            }
            return list
        }
    private val picked by lazy {
        try {
            defaultJson.decodeFromString(getOrCreateDataFile("picked.json").readText())
        } catch (e: Exception) {
            mutableListOf<Long>()
        }
    }

    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val (i, matchResult) = eventMatchResult.getIndexedResult()
        val cakes = moonCakes
        if (i == 1) {
            sendMessage(
                buildMessageChain {
                    cakes.onEach {
                        +PlainText(it.name + ": " + it.emoji.ifBlank { "\uD83E\uDD6E" }.repeat(it.count)+"\n")
                    }.ifEmpty {
                        +PlainText("我草，被拿完了！！！")
                    }
                }
            )
            return ExecutionResult.Success
        }
        if (picked.contains(sender.id)){
            sendMessage("不许拿了！！！，你已经拿过了！！！")
            return ExecutionResult.LimitCall
        }
        if (picking.get()) {
            sendMessage("别急,有人在拿")
            return ExecutionResult.LimitCall
        }
        val cakeName = matchResult.groupValues[1]
        if (cakeName.isBlank()&&i==0) {
            sendMessage("你个申比,你让我拿空气？")
            return ExecutionResult.LimitCall
        }
        if (cakes.any { it.name.equals(cakeName.trim(), true) }.not()) {
            sendMessage(
                buildMessageChain {
                    +PlainText("我找不见$cakeName！！\n")
                    cakes.onEach {
                        +PlainText(it.name + ": " + it.emoji.ifBlank { "\uD83E\uDD6E" }.repeat(it.count)+"\n")
                    }.ifEmpty {
                        +PlainText("我草，被拿完了！！！")
                    }
                }
            )
            return ExecutionResult.LimitCall
        }
        val cake = cakes.find { it.name.equals(cakeName.trim(), true) }!!
        if (cake.count <= 0) {
            sendMessage("我草！！${cakeName}被拿完了！！！")
        }
        cakes.replaceAll {
            if (it.name.equals(cakeName.trim(), true)) {
                it.copy(count = it.count - 1)
            } else it
        }
        picked.add(sender.id)
        sendMessage("好了！!你已经拿走了一个$cakeName 月饼！！！")
        saveCake(cakes)
        return ExecutionResult.Success
    }

    @Synchronized
    fun saveCake(cakes: MutableList<Cake>) {
        getOrCreateDataFile("picked.json").writeText(defaultJson.encodeToString(picked))
        getOrCreateDataFile("cakes.json").writeText(defaultJson.encodeToString(cakes))
    }

    @kotlinx.serialization.Serializable
    data class Cake(
        val name: String,
        val emoji: String,
        val count: Int
    ) {

    }
}