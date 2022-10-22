package org.celery.command.common.funny

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.data.TempData
import org.celery.utils.sendMessage
import java.net.URLEncoder

object BaiduSearchSBCommand: Command(
    "百度一下",
    usage = "百度一下 <你要搜索的东西>",
    description = "帮助残疾人捏"
){

    @Command("^百度一?下\\s*(.+)")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val count = TempData[commandId+"."+sender.id,0]
        TempData[commandId+"."+sender.id] = count.plus(1)
        if (count>3){
            sendMessage(At(sender)+PlainText("？真就他妈当残疾人了是吧"))
            return  ExecutionResult.Success
        }
        sendMessage("https://buhuibaidu.me/?s="+ withContext(Dispatchers.IO){
            URLEncoder.encode(eventMatchResult[1],"utf8")
        })
        return ExecutionResult.Success
    }

}