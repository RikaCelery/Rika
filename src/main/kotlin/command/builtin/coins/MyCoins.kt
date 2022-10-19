package command.builtin.coins

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.abs.Command
import org.celery.data.Coins
import org.celery.data.TempData
import org.celery.utils.number.probability
import org.celery.utils.number.randomInt
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder

object MyCoins : Command("我的原石") {


    @Command("^我的原石\$")
    suspend fun MessageEvent.handle(): ExecutionResult {
        val key = commandId+"."+sender.id.toString()
        val index = TempData[ key, 0]
        when (index) {
            0 -> handle0()
            else -> handle1()
        }
        TempData[key] = index + 1
        return ExecutionResult.Success
    }

    private suspend fun MessageEvent.handle0() {

        sendMessage(buildString {
            if (probability(config["additional_coins_probability",0.6])) {
                append(config["coin_now","你现在有{coins}个原石"].placeholder(
                    "coins" to Coins[sender]
                ))
                val randomInt = randomInt(config["additional_coins_min", 50], config["additional_coins_max", 2000])
                Coins[sender] += randomInt
                append(config["if_lucky","\n欸嘿我今天心情好再给你点原石"].placeholder(
                    "coins" to Coins[sender],
                    "increasement" to randomInt
                ))
            }
            append(config["coin_now","你现在有{coins}个原石"].placeholder(
                "coins" to Coins[sender]
            ))
        })
    }

    private suspend fun MessageEvent.handle1() {

        sendMessage(buildString {
            append(config["coin_now","你现在有{coins}个原石"].placeholder(
                "coins" to Coins[sender]
            ))
        })
    }
}