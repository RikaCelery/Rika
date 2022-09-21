package org.celery.command.common.coins

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.Call
import org.celery.command.controller.RegexCommand
import org.celery.data.Coins
import org.celery.utils.number.probability
import org.celery.utils.number.randomInt
import org.celery.utils.sendMessage

object MyCoins: RegexCommand("我的原石", "^我的原石$".toRegex(), normalUsage = "我的原石") {
    init{
        defaultCallCountLimitMode = BlockRunMode.PureUser
    }


    @Command
    suspend fun MessageEvent.handle() {

        sendMessage(buildString {
            append("你现在有${Coins[sender]}个原石")
            if (probability(0.6)){
                Coins[sender]+= randomInt(50,1000)
                append("\n")
                append("欸嘿我今天心情好再给你点原石")
                append("\n")
                append("你现在有${Coins[sender]}个原石")
            }
        })
    }

    @Command(repeat = 2)
    suspend fun MessageEvent.handle2() {

        sendMessage(buildString {
            append("你现在有${Coins[sender]}个原石")
        })
    }
    override suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {
//        val contact = getGroupOrFail(call.subjectId!!)
//        contact.sendMessage(
//            buildMessageChain {
//                val random = Random(call.userId!!+TimeUtils.getNowYear()+TimeUtils.getNowMonth()+TimeUtils.getNowDay())
//                append("你人品值%.1f 别来烦我了！！！！".format(random.nextInt(0,1000).toFloat().div(10)))
//            }
//        )
    }
}