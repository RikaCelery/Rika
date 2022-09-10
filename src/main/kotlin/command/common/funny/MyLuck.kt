package org.celery.command.common.funny

import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.RegexCommand
import org.celery.data.Coins
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeUtils
import kotlin.random.Random

object MyLuck:RegexCommand("今日运气","/?vdluck".toRegex(), normalUsage = "vdluck") {
    override var defaultCountLimit: Int = 8
    override var defaultCallCountLimitMode: BlockRunMode = BlockRunMode.User
    private val rander = Random
    @Command
    suspend fun MessageEvent.handle(){
        val random = Random(sender.id+TimeUtils.getNowYear()+TimeUtils.getNowMonth()+TimeUtils.getNowDay())
        sendMessage(buildString {
            append("今天你的运气值是:%.1f".format(random.nextInt(0,1000).toFloat().div(10)))
            append("\n")
            append("我草，顺便给你点原石")
            Coins[sender]+= rander.nextInt(300,2000)
            append("\n")
            append("现在你有${Coins[sender]}个原石！！!")
        })
    }
    @Command(repeat = 2)
    suspend fun MessageEvent.handle2(){
        val random = Random(sender.id+TimeUtils.getNowYear()+TimeUtils.getNowMonth()+TimeUtils.getNowDay())
        sendMessage(buildString {
            append("他妈 你个逼失忆了？不都跟你说过今天你人品值是%.1f了嘛！".format(random.nextInt(0,1000).toFloat().div(10)))
        })
    }

    @Command(repeat = 3)
    suspend fun MessageEvent.handle3(){
        val random = Random(sender.id+TimeUtils.getNowYear()+TimeUtils.getNowMonth()+TimeUtils.getNowDay())
        sendMessage(buildString {
            append("草你吗 你失忆了？？？你人品值是他妈的%.1f".format(random.nextInt(0,1000).toFloat().div(10)))
        })
    }
    @Command(repeat = 4)
    suspend fun MessageEvent.handle4(){
        sendMessage(buildString {
            append("还问？还问？还问？我刚刚不是说了？？？？")
        })
    }
    @Command(repeat = 5)
    suspend fun MessageEvent.handle5(){
        sendMessage(buildString {
            append("我是你爹啊什么都得听你的 我就不他妈的说")
        })
    }
    @Command(repeat = 6)
    suspend fun MessageEvent.handle6(){
        sendMessage(buildString {
            append("别来烦我了！！！操你妈")
        })
    }
}