package org.celery.command.common.ero.commands

import command.common.ero.SetuLibManager
import events.ExecutionResult
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.Call
import org.celery.command.controller.RegexCommand
import org.celery.exceptions.CommandAbortException

object RandomSetu : RegexCommand(
    "随机涩图",
    "^来张(涩?图|setu)$".toRegex(),
    5,
    "来张图",
    "随机涩图",
    "来张图\n来张涩图\n来张setu\n随机涩图\n随机setu",
    "",
    "",
    "^随机涩图$".toRegex(),
    "^随机setu$".toRegex(),

) {
    override var blockSub: Boolean = true
    init{
        defaultCountLimit = 8
        defaultCallCountLimitMode = BlockRunMode.PureUser
        defaultCoolDown = 2000
    }

    @Command
    suspend fun MessageEvent.handle(): ExecutionResult {
        val setuLib = SetuLibManager.random()
        logger.debug(setuLib.libName)
        val setu = setuLib.getSetuOrNull(sender, subject)
        logger.debug(setu.toString())
        logger.debug(setu?.getFiles()?.map { it.name }.toString())
        val sendResult = setu?.sendTo(null, subject)
        logger.debug(sendResult.toString())
        if(sendResult ==true)
        return ExecutionResult.Success
        else
        return ExecutionResult.Failed(null,"unknown")
    }

//    @Command(repeat = 6)
//    suspend fun MessageEvent.handle6() {
//        sendMessage(buildString {
//            append("我觉得吧，差不多可以了")
//        })
//    }
//
//    @Command(repeat = 7)
//    suspend fun MessageEvent.handle8(): ExecutionResult {
//        if(SetuLibManager.random().getSetuOrNull(sender, subject)?.sendTo(null,subject)==true)
//            return ExecutionResult.Success
//        else
//            return ExecutionResult.Failed(null,"unknown")
//    }
    override suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {
        addCall(call)

        val contact = getGroupOrFail(call.subjectId!!)
        contact.sendMessage(
            buildMessageChain {
                append("不给力！！！😡")
            }
        )
        throw CommandAbortException()
    }
}