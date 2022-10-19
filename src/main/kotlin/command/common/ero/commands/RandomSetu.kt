package org.celery.command.common.ero.commands

import command.common.ero.EroConfig
import events.ExecutionResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.source
import org.celery.command.common.ero.impl.SetuPixivLazyLib
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.throwOnFailure
import org.celery.command.controller.abs.withlock
import org.celery.utils.sendMessage

object RandomSetu : Command(
    "随机涩图",5,"涩图"
) {
    @Command("^(来张|随机)(涩?图|setu)$", coin = 800)
    suspend fun MessageEvent.handleRandmom1(): ExecutionResult = random()

    private suspend fun MessageEvent.random(): ExecutionResult {
        withlock(0,sender.id){
            val setuLib = SetuPixivLazyLib
            val setu = setuLib.getSetuOrNull(sender, subject)
            if (setu == null) {
                sendMessage("你要不还是别看了？")
                return ExecutionResult.LimitCall
            }
            val minutes = EroConfig["指定涩图.execute_timeout_minutes", 1.0]
            val seconds = minutes.times(60).toLong()
            try {
                val sendResult =  withTimeout(seconds*1000){
                    setu.sendTo(message.source, subject)
                }
                return if (sendResult)
                    ExecutionResult.Success
                else
                    ExecutionResult.Failed(null, "发送图片失败")
            } catch (e: TimeoutCancellationException) {
                return ExecutionResult.Failed(e,"图片下载超时(${minutes}minutes)，已取消")
            }
        }.onLocked {
            sendMessage("急你妈")
            return ExecutionResult.LimitCall
        }.throwOnFailure()
        return ExecutionResult.Unknown
    }


}