package org.celery.command.common.funny

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.withlock
import org.celery.data.Coins
import org.celery.data.TempData
import org.celery.utils.contact.simpleStr
import org.celery.utils.number.probability
import org.celery.utils.number.randomInt
import org.celery.utils.sendMessage

object BegForCoins : Command(
    "乞讨"
) {
    @Command("^乞讨$|^给我?点原石吧?$")
    suspend fun MessageEvent.handle(): ExecutionResult {
        withlock(subject.id, 0) {
            val key = commandId + "." + sender.simpleStr
            val index = TempData[key, 0]
            when (index) {
                in 0..config["max", 5] -> handleDefault(index)
                else -> handleElse()
            }
            TempData[key] = index + 1
            return ExecutionResult.Success
        }.onLocked {
            return ExecutionResult.LimitCall
        }
        return ExecutionResult.Success
    }

    private suspend fun MessageEvent.handleDefault(index: Int) {
        sendMessage(buildString {
            val coins = if (probability(0.1)) {
                0
            } else randomInt(10, 2000.div(index.coerceAtLeast(1)).coerceAtLeast(10))
            when (coins) {
                in 1000 until 2000 -> {
                    append(config["reply_in_1000-2000", "嘻嘻，给~"])
                }
                in 200 until 1000 -> {
                    append(config["reply_in_200-1000", "行吧行吧，给你点"])
                }
                in 1 until 200 -> {
                    append(config["reply_in_1-200", "噫~，给给给，离我远点！\uD83D\uDE21"])
                }
                0 -> {
                    append(config["reply_at_0", "爬爬爬😡"])
                }
            }
            Coins[sender]+=coins
            append("你现在有${Coins[sender]}个原石")
        })
    }

    suspend fun MessageEvent.handleElse() {
        sendMessage(buildString {
            append(config["reply_else", "爬，赶紧爬"])
        })
    }
}