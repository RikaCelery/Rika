package org.celery.command.common.ero.commands

import events.ExecutionResult
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.source
import org.celery.command.common.ero.impl.SetuPixivLazyLib
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.Call
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.exceptions.CommandAbortException
import org.celery.utils.sendMessage

object SpecificSetu : RegexCommand(
    "指定涩图",
    "^来张\\s*(.+)$".toRegex(),
    4,
    "来张atri",
    "指定tag涩图",
    "来张atri",
    "",
    "",
    "^tag\\s*(.+)$".toRegex(),

) {
    init{
        defaultCountLimit = 8
        defaultCallCountLimitMode = BlockRunMode.PureUser
        defaultCoolDown = 2000
    }

    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val tag = eventMatchResult[1]
        val setuLib = SetuPixivLazyLib
        logger.debug(setuLib.libName)
        val setu = setuLib.getSetuOrNull(sender, subject,tag)
        if (setu==null){
            sendMessage(
            QuoteReply(message.source)+PlainText("图库里没有"))
            return ExecutionResult.LimitCall
        }
        logger.debug(setu.toString())
        logger.debug(setu.getFiles().map { it.name }.toString())
        val sendResult = setu.sendTo(message.source, subject)
        logger.debug(sendResult.toString())
        return if(sendResult)
            ExecutionResult.Success
        else
            ExecutionResult.Failed(null,"unknown")
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