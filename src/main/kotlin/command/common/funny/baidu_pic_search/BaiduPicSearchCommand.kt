package command.common.funny.baidu_pic_search

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.safeCast
import org.celery.command.common.baidu_pic_search.DataBaiduPicSearch
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.throwOnFailure
import org.celery.command.controller.abs.withlock
import org.celery.utils.contact.GroupUploadTool
import org.celery.utils.file.FileTools
import org.celery.utils.http.HttpUtils
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import java.net.URLEncoder

object BaiduPicSearchCommand : Command(
    "百度搜图", 5, "实用工具"
) {
    val warnings = mutableMapOf<Long?, Int>()
    @Command("^搜图\\s*(.+)",coin = 100)
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val withlock = withlock(subject.id, 0) {
            executionResult(eventMatchResult, false)
        }
        withlock.onLocked {
            sendMessage(config["command_is_using","急你妈"])
        }.throwOnFailure()
        return if (withlock.isLocked) ExecutionResult.LimitCall
        else ExecutionResult.Success
    }

    @Command("^带链接搜图\\s*(.+)",coin = 100)
    suspend fun MessageEvent.handle2(eventMatchResult: EventMatchResult): ExecutionResult {
        val withlock = withlock(subject.id, 0) {
            executionResult(eventMatchResult, true)
        }
        withlock.onLocked {
            sendMessage(config["command_is_using","急你妈"])
        }.throwOnFailure()
        return if (withlock.isLocked) ExecutionResult.LimitCall
        else ExecutionResult.Success
    }

    private suspend fun MessageEvent.executionResult(
        eventMatchResult: EventMatchResult,
        file: Boolean
    ): ExecutionResult {
        var target = eventMatchResult[1]
        var page = ""
        "(.+)\\s+(\\d+)".toRegex().find(target)?.let {
            target = it.groupValues[1]
            page = it.groupValues[2]
        }
        if (target.isBlank())
            return ExecutionResult.Ignored("blank target")
        if (page == "0" && (sender.isSuperUser() || sender.safeCast<NormalMember>()?.isOperator() == true)) {
            HttpUtils.MyCookieJar.clearCookie()
            HttpUtils.clear()
            sendMessage("重置cookie和cache成功")
            return ExecutionResult.Success
        }
        if (page.isNotEmpty() && page.toIntOrNull() == null) {
            sendMessage(page + "不是有效的页码")
            return ExecutionResult.LimitCall
        }
        if (!canSearch(target) && sender.isSuperUser().not()) {
            if (warnings[sender.id] == 1) {
                //                    sender?.id?.let { BlackList.addUser(senderId = it, 3 * TimeUtil.DAY * 1000L) }
            } else {
                if (warnings[sender.id] == 0) {
                    warnings[sender.id] = 1
                } else {
                    warnings[sender.id] = 0
                }
                sendMessage("该关键词禁止搜索,警告${warnings[sender.id]}次")
            }
            return ExecutionResult.LimitCall
        }
        val param = withContext(Dispatchers.IO) {
            URLEncoder.encode(target, "utf8")
        }
        BaiduPicSearchCommand.logger.info("搜索图片:$target,$param")
        if (page == "") {
            val url =
                "https://image.baidu.com/search/acjson?tn=resultjson_com&pn=${(0..10).random() * 20}&rn=20&word=$param"
            val jsonString = HttpUtils.getStringContent(url, true).replace("\\'", "'")
            val result = defaultJson.decodeFromString(DataBaiduPicSearch.serializer(), jsonString)

            try {
                generateMessage(result)
            } catch (e: Exception) {
                getOrCreateDataFile("error.txt")
                    .appendText(defaultJson.encodeToString(DataBaiduPicSearch.serializer(), result))
                throw e
            }
        } else {
            val url = "https://image.baidu.com/search/acjson?tn=resultjson_com&pn=${
                page.toInt().minus(1) * 20
            }&rn=20&word=$param"
            val jsonString = HttpUtils.getStringContent(url, true).replace("\\'", "'")
            val result = try {
                defaultJson.decodeFromString(DataBaiduPicSearch.serializer(), jsonString)
            } catch (e: Exception) {
                //                sendMessage("出错了喵(。>︿<)_θ")
                //                logger.error(e.stackTraceToString())
                return ExecutionResult.Failed(e)
            }
            try {
                if (file)
                    generateLinkFileMessage(result, "$target$page.txt")
                generateForawrdMessage(result, page)
            } catch (e: Exception) {
                getOrCreateDataFile("error.txt")
                    .appendText(defaultJson.encodeToString(DataBaiduPicSearch.serializer(), result))
                throw e
            }
        }
        return ExecutionResult.Success
    }


    private fun canSearch(target: String): Boolean {
        BaiduPicSearchData.regexList.forEach {
            try {
                if (Regex(it).containsMatchIn(target)) {
                    return false
                }
            } catch (_: Exception) {

            }
        }
        return true
    }

    private suspend fun MessageEvent.generateMessage(result: DataBaiduPicSearch) {
        result.data.filter { it.getOriLink() != null }.randomOrNull()?.runCatching {
            try {
                getOriLink()?.let { it1 ->
                    HttpUtils.downloader(it1).inputStream().toExternalResource().use { resource ->
                        sendMessage(PlainText(getTitle()) + subject.uploadImage(resource))
                    }
                }
            } catch (e: Exception) {
                this.thumbURL.let {
                    HttpUtils.downloader(it).inputStream().toExternalResource().use { resource ->
                        sendMessage(PlainText(getTitle() + "\n[thumb]\n") + subject.uploadImage(resource))
                    }
                }
            }
        } ?: run {
            println(Json.encodeToJsonElement(DataBaiduPicSearch.serializer(), result))
            sendMessage("什么都没有呢")
        }
    }

    private suspend fun MessageEvent.generateForawrdMessage(
        result: DataBaiduPicSearch,
        page: String
    ) {
        val builder = ForwardMessageBuilder(subject)
        builder.add(
            sender,
            PlainText("您搜索了:${result.queryWord}\n当前是第${page}页\n共${result.totalNum / 20 + 1}页\n已展示20个结果\n需要链接可以在页数后面添加true来上传链接")
        )
        result.data.filter { it.getOriLink() != null }.ifEmpty { null }?.forEach {
            builder.add(sender, it.getImageMessage(sender, subject))
        } ?: run {
            println(Json.encodeToJsonElement(DataBaiduPicSearch.serializer(), result))
            builder.add(sender, PlainText("什么都没有呢"))
        }
        sendMessage(builder.build())
    }

    private suspend fun MessageEvent.generateLinkFileMessage(
        result: DataBaiduPicSearch,
        fileName: String
    ) {
        val r = FileTools.creatTempFile()
        r.writeText(result.data.filter { it.getOriLink() != null }.ifEmpty { null }?.map { it.getInfo() }
            ?.joinToString("\n") ?: return)
        subject.safeCast<Group>()
            ?.let { GroupUploadTool.uploadFile(group = it, r, fileName, allowOverwrite = true, autoDelete = true) }

    }
}