package org.celery.command.common.group.`fun`.banme

import events.ExecutionResult
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import org.celery.command.controller.RegexCommand
import org.celery.utils.interact.nextMessage
import org.celery.utils.sendMessage

object Banme : RegexCommand(
    "banme", "^/?banme".toRegex(), description = "娱乐禁言",
    normalUsage = "banme",
) {
    private val rander = kotlin.random.Random
    override var defaultCoolDown: Long = 1000

    @Command
    suspend fun GroupMessageEvent.on(): ExecutionResult {
        if (!group.botAsMember.isOperator()) {
            return ExecutionResult.Ignored("not operator.")
        }
        if (sender.isOperator() && !group.botAsMember.isOwner()) {
            group.sendMessage("?,建议把群主给我捏")
            return ExecutionResult.LimitCall
        }
        when (rander.nextInt(1001)) {
            in 0 until 500 -> {
                randomMute()
            }
            in 700 until 999 -> {
                selectMute()
            }
            1000 -> {
                permentMute()
            }
        }
        return ExecutionResult.Success
    }


    private suspend fun GroupMessageEvent.randomMute() {
        var time = rander.nextInt(100, 801)
        when (time) {
            in 660..700 -> {
                time = rander.nextInt(5000, 7000)
                group.sendMessage("恭喜你中了一等奖,请留下你的遗言😭,倒计时5s")
                delay(6000)
                sender.mute(time)
                delay(500)
                group.sendMessage(PlainText("恭喜") + At(sender) + PlainText("获得${time}秒的禁言大礼包"))
            }
            else -> {
                sender.mute(time)
                group.sendMessage(PlainText("恭喜") + At(sender) + PlainText("获得${time}秒的禁言礼包"))
            }
        }
    }

    private suspend fun GroupMessageEvent.selectMute() {
        val selection = nextMessage("请问你要这个金禁言(🥇)呢，还是这个银禁言(🥈)呢，都不选的话我只能让你永远闭嘴惹！", 60, autoRecall = false) {
            it.content.contains("🥇") || it.content.contains("🥈") || it.content.contains("金") || it.content.contains("银")
        } ?: kotlin.run {
            permentMute()
            return
        }

        when {
            selection.content.contains("🥇") || selection.content.contains("金") -> {
                sender.mute(5000)
                sendMessage(PlainText("恭喜") + At(sender) + PlainText("获得5000秒的禁言🎉"))
            }
            selection.content.contains("🥈") || selection.content.contains("银") -> {
                when (rander.nextInt(0, 6)) {
                    in 0..4 -> {
                        sender.mute(800)
                        group.sendMessage(PlainText("恭喜") + At(sender) + PlainText("获得800秒的禁言🎉"))
                    }
                    else -> {
                        sender.mute(5000)
                        delay(500)
                        sendMessage(PlainText("嘻嘻,想得美"))
                        delay(500)
//                        sendMessage(PlainText("恭喜")+ At(sender) +PlainText("获得5000秒的禁言🎉"))
                    }
                }
            }
            else -> {
                sendMessage("?")
                permentMute()
            }
        }

    }

    private suspend fun GroupMessageEvent.permentMute() {
        sender.mute(114514)
        delay(500)
        sendMessage(PlainText("😋😋114514s后见") + At(sender))
    }
}
