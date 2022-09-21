package org.celery.command.common.funny

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage
import java.net.URLEncoder

object BaiduSearchSBCommand: RegexCommand(
    "百度一下",
    "^百度一?下\\s*(.+)".toRegex(),
    normalUsage = "百度一下 <你要搜索的东西>",
    description = "帮助残疾人捏"
){
    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        sendMessage("https://buhuibaidu.me/?s="+ withContext(Dispatchers.IO){
            URLEncoder.encode(eventMatchResult[1],"utf8")
        })
        return ExecutionResult.Success
    }

    override val limitMessage: String?
        get() = "？真就他妈当残疾人了是吧"
}