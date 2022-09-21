package org.celery.command.common.nbnhhsh

import events.ExecutionResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.FormBody
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium

object Nbnhhsh : RegexCommand(
    commandId = "能不能好好说话",
    regex = "^[?？]\\s*([a-zA-Z\\s]+)".toRegex(),
    normalUsage = "?<缩写>",
    description = "尝试猜测缩写的含义\n  (老了老了,现在上个网是真看不懂这群人在聊什么东西)",
    example = "?nbnhhsh"
) {
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val matchResult = eventMatchResult.getResult().groupValues.last()
        var nbnhhshItem: DataClassNbnhhshItem? = null
        try {
            val formBody = FormBody.Builder().add("text", matchResult).build()
            val req = Request.Builder().url("https://lab.magiconch.com/api/nbnhhsh/guess").post(formBody).build()
            val res = HttpUtils.withClient { newCall(req).execute() }
            nbnhhshItem = jsonSerializer.parseToJsonElement(res.body!!.string()).jsonArray.ifEmpty { null }?.map {
                jsonSerializer.decodeFromJsonElement(
                    DataClassNbnhhshItem.serializer(), it
                )
            }?.first()
            res.close()

            val resultList = nbnhhshItem?.result
            if (resultList?.second.isNullOrEmpty()) {
                group.sendMessage("我不知道（〃｀ 3′〃）")
                return ExecutionResult.Ignored("not found result")
            }
            SharedSelenium.renderRaw(renderHtml(resultList!!.first, resultList.second)).toExternalResource().use {
                group.uploadImage(it)
            }.sendTo(group)
            return ExecutionResult.Success
        } catch (e: Exception) {
//            ErrorReport.report(e.toString())
            logger.error(e.stackTraceToString() + nbnhhshItem.toString())
            return ExecutionResult.Failed(Exception(nbnhhshItem.toString(), e))
        }
    }

    private fun renderHtml(first: String, second: List<String>): String {
        return """
            
            <body>
                <h1>${StringEscapeUtils.escapeHtml4(first)} 可能是:</h1>
                <div>
                    ${second.joinToString("") { "<div>${StringEscapeUtils.escapeHtml4(it)}</div>" }}
                </div>
            </body>
            <style>
                h1{
                    text-align: center;
                    font-size: 4pc;
                    margin: 0px;
                    padding: 0px;
                    color: rgb(62, 126, 177);
                }
                body{
                    font-size: 35px;
                    background-color: rgb(238, 238, 238);
                }
                body > div {
                    display: flex;
                    flex-wrap: wrap;
                    color: rgb(252, 248, 248);
                    padding: 10px;
                    border-style: dashed;
                    border-width: 5px;
                    border-color: rgb(18, 161, 233);
                    border-radius: 30px;
                    margin: 10px;
                    box-sizing: border-box;
                }
                body > div> * {
                    /* border-style: solid; */
                    /* border-style: groove; */
                    /* border-width: 2px; */
                    border-radius: 15px;
                    /* margin-bottom: 5px; */
                    margin: 5px;
                    box-sizing: border-box;
                    padding-left: 8px;
                    padding-right: 8px;
                    padding-top: 0px;
                    padding-bottom: 4px;
                    /* border-width: 1px; */
                    background-color: rgba(70, 125, 159, 0.325);

                    text-shadow: 2px 2px 4px #0c0c0cb7;
                    box-shadow: 6px 6px 20px #ffffff3a;
                }
            </style>
        """.trimIndent()
    }

}