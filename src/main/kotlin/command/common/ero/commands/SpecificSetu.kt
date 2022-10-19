package org.celery.command.common.ero.commands

import command.common.ero.EroConfig
import command.common.ero.SetuLibManager
import events.ExecutionResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.source
import org.celery.command.common.ero.impl.SetuPixivLazyLib
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.*
import org.celery.utils.sendMessage

object SpecificSetu : Command(
    "指定涩图", 4, classify = "涩图"

) {

    override val blockSub: Boolean = true

    @Command("^(?:来张|tag)\\s*(.+)$", coin = 1000)
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        withlock(0, sender.id){
            val minutes = EroConfig["指定涩图.execute_timeout_minutes", 1.0]
            val seconds = minutes.times(60).toLong()
            val tag = eventMatchResult[1]

            val setu =
                // try web lib and local lib (matches alias)
                SetuLibManager.getLib(tag)?.getSetuOrNull(sender, subject) ?:
                // if not found user pixiv-lib (tags search)
                SetuPixivLazyLib.getSetuOrNull(sender, subject, tag)
            if (setu == null) {
                sendMessage(
                    QuoteReply(message.source) + PlainText("图库里没有"))
                return@withlock ExecutionResult.Ignored
            }
            logger.debug(setu.getFiles().map { it.name }.toString())
            try {
                val sendResult = withTimeout(seconds * 1000) {
                    setu.sendTo(message.source, subject)
                }
                return@withlock if (sendResult)
                    ExecutionResult.Success
                else
                    ExecutionResult.Failed(null, "发送图片失败")
            } catch (e: TimeoutCancellationException) {
                return@withlock ExecutionResult.Failed(e, "图片下载超时(${minutes}minutes)，已取消")
            }
        }.onLocked {
            sendMessage("急你妈")
            return ExecutionResult.LimitCall
        }.onSuccess {
            return it
        }.throwOnFailure()
        return ExecutionResult.Success
    }
}