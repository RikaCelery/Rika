package command.common.funny.baidu_pic_search

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.sendMessage

object BaiduPicSearchBlackWords : Command(
    "搜图屏蔽词"
) {
    @Command("添加搜图屏蔽词\\s(.*)", permissions = [RequirePermission.MEMBER, RequirePermission.FRIEND, RequirePermission.SUPERUSER])
    suspend fun MessageEvent.handleAdd(eventMatchResult: EventMatchResult): ExecutionResult {
        val regex = eventMatchResult[1].trim().toRegex()
        if (regex.containsMatchIn("  ")) {
            sendMessage("你不能添加会匹配任何字符的正则")
            return ExecutionResult.Ignored("illegal regex")
        }
        if (!BaiduPicSearchData.regexList.contains(regex.pattern)) {
            BaiduPicSearchData.regexList.add(regex.pattern)
            sendMessage("OK")
        }else
            sendMessage("正则已存在")
        return ExecutionResult.Success
    }

    @Command("\"(?:移除|删除)搜图屏蔽词\\\\s(.*)\"", permissions = [RequirePermission.MEMBER, RequirePermission.FRIEND, RequirePermission.SUPERUSER])
    suspend fun MessageEvent.handleDelete(eventMatchResult: EventMatchResult): ExecutionResult {
        val regex = eventMatchResult[1].trim().toRegex()
        if (BaiduPicSearchData.regexList.contains(regex.pattern)) {
            BaiduPicSearchData.regexList.remove(regex.pattern)
        sendMessage("OK")
        }else
            sendMessage("没有这个正则")
        return ExecutionResult.Success
    }
}