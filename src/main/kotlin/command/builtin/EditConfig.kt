package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.config.main.PublicConfig
import org.celery.utils.interact.getConfirm
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.toImage
import org.openqa.selenium.Dimension

object EditConfig:RegexCommand(
    "修改配置","^修改\\s(.+)\\s(.+)".toRegex()
) {
    @Command(ExecutePermission.SuperUser)
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult.Ignored {
        val key = eventMatchResult[1]
        val value = eventMatchResult[2]
        if (PublicConfig[key] ==null){
            sendMessage(PlainText("指定key不存在")+SharedSelenium.render(PublicConfig.entries.joinToString("\n"){it.key}, dimension = Dimension(800,0)).toImage(subject))
            return ExecutionResult.Ignored()
        }
        if (getConfirm("${PublicConfig[key]}=>$value")){
            PublicConfig[key]=value
            sendMessage("成功, value:${PublicConfig[key]}")
        }
        sendMessage("取消, value:${PublicConfig[key]}")
        return ExecutionResult.Ignored()
    }
}