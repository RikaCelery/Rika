package command.common.tool.github

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.throwOnFailure
import org.celery.command.controller.abs.withlock
import org.celery.utils.http.HttpUtils
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage
import org.intellij.lang.annotations.Language
import java.net.URLEncoder

object Github : Command(
    "github",
    5,
    "实用工具",
    "github仓库查询",
    "github  <仓库名称>",
    "maxresult: 一次性最多返回多少个仓库信息\nshow_link: 发送仓库全名",
    ""
)   {
    private const val githubAPI = "https://api.github.com/search/repositories"

    @Language("RegExp")
    @Command("""^github\s+(.+)""")
    suspend fun MessageEvent.handle1(eventMatchResult: EventMatchResult): ExecutionResult {
        withlock(subject.id, 0){//锁定群
            val array = getResult(eventMatchResult)
            if (array == null) {
                sendMessage(config["repo_not_found","我草，我也不知道"])
                return ExecutionResult.LimitCall
            }
            delay(config["delay_after_get",2000])
            sendMessage(getMessages(array))//生成消息并发送
        }.onLocked {//在上一次指令未执行完成时再次使用
            sendMessage(config["duplicate_call","急你妈"])
            temporaryBan(subject.id,sender.id,10*1000)//强制禁用 10s
            return ExecutionResult.LimitCall
        }.throwOnFailure()//报错时抛出异常，由调用函数处理
        return ExecutionResult.Success
    }

    private suspend fun MessageEvent.getMessages(array: JsonArray): MessageChain {
        return buildMessageChain {
            array.forEachIndexed { index, element ->
                +HttpUtils.downloader("https://opengraph.githubassets.com/0/" + element.jsonObject["full_name"]!!.jsonPrimitive.content)
                    .toImage(subject)
                if (config.get("show_link", false))
                    +PlainText("https://github.com/"+element.jsonObject["full_name"]!!.jsonPrimitive.content)
                else if (config.get("show_full_name", true))
                    +PlainText(element.jsonObject["full_name"]!!.jsonPrimitive.content)
            }
        }
    }

    private suspend fun getResult(eventMatchResult: EventMatchResult): JsonArray? {
        val content = HttpUtils.getStringContent(
            "%s?q=%s&per_page=%d".format(
                githubAPI,
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(eventMatchResult[1], "utf8")
                },
                config.get("maxresult", 1)
            ), true
        )
        val jsonElement = defaultJson.parseToJsonElement(content).jsonObject
        val array =
            if (jsonElement["total_count"]!!.jsonPrimitive.int != 0) jsonElement.jsonObject["items"]!!.jsonArray else null
        return array
    }
}
