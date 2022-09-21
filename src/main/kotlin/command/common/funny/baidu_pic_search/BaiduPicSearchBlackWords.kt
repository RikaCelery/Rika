package command.common.funny.baidu_pic_search

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage

object BaiduPicSearchBlackWords : RegexCommand(
    "搜图屏蔽词", "添加搜图屏蔽词\\s(.*)".toRegex(), secondaryRegexs = arrayOf("(?:移除|删除)搜图屏蔽词\\s(.*)".toRegex())
) {
    @Command(ExecutePermission.Operator)
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val regex = eventMatchResult[1].trim().toRegex()
        if (regex.containsMatchIn("  ")) {
            sendMessage("你不能添加会匹配任何字符的正则")
            return ExecutionResult.Ignored("illegal regex")
        }
        if (!BaiduPicSearchData.regexList.contains(regex.pattern)) {
            BaiduPicSearchData.regexList.add(regex.pattern)
        }
        sendMessage("OK")
        return ExecutionResult.Success
    }

}