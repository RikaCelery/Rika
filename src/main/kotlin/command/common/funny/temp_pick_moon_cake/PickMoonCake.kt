package command.common.funny.temp_pick_moon_cake

import events.ExecutionResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import java.util.concurrent.atomic.AtomicBoolean

object PickMoonCake : Command("月饼", usage = "拿月饼<月饼名字>\n查月饼: 返回所有月饼") {

    private val picking = AtomicBoolean(false)
    private val moonCakes: MutableList<Cake>
        get() {
            val list = try {
                defaultJson.decodeFromString(getOrCreateDataFile("cakes.json").readText())
            } catch (e: Exception) {
                mutableListOf<Cake>()
            }
            return list
        }
    private val picked by lazy {
        try {
            defaultJson.decodeFromString(getOrCreateDataFile("picked.json").readText())
        } catch (e: Exception) {
            mutableListOf<Long>()
        }
    }

    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val (i, matchResult) = eventMatchResult.getIndexedResult()
        val cakes = moonCakes
        if (i == 1) {
            sendMessage(buildMessageChain {
                cakes.onEach {
                    +PlainText(it.name + ": " + it.emoji.ifBlank { "\uD83E\uDD6E" }.repeat(it.count) + "\n")
                }.ifEmpty {
                    +PlainText("我草，被拿完了！！！")
                }
            })
            return ExecutionResult.Success
        }
        if (picked.contains(sender.id)) {
            sendMessage("不许拿了！！！，你已经拿过了！！！")
            return ExecutionResult.LimitCall
        }
        if (picking.get()) {
            sendMessage("别急,有人在拿")
            return ExecutionResult.LimitCall
        }
        val cakeName = matchResult.groupValues[1]
        if (cakeName.isBlank() && i == 0) {
            sendMessage("你个申比,你让我拿空气？")
            return ExecutionResult.LimitCall
        }
        if (cakes.any { it.name.equals(cakeName.trim(), true) }.not()) {
            sendMessage(buildMessageChain {
                +PlainText("我找不见$cakeName！！\n")
                cakes.onEach {
                    +PlainText(it.name + ": " + it.emoji.ifBlank { "\uD83E\uDD6E" }.repeat(it.count) + "\n")
                }.ifEmpty {
                    +PlainText("我草，被拿完了！！！")
                }
            })
            return ExecutionResult.LimitCall
        }
        val cake = cakes.find { it.name.equals(cakeName.trim(), true) }!!
        if (cake.count <= 0) {
            sendMessage("我草！！${cakeName}被拿完了！！！")
        }
        cakes.replaceAll {
            if (it.name.equals(cakeName.trim(), true)) {
                it.copy(count = it.count - 1)
            } else it
        }
        picked.add(sender.id)
        sendMessage("好了！!你已经拿走了一个$cakeName 月饼！！！")
        saveCake(cakes)
        return ExecutionResult.Success
    }

    @Synchronized
    fun saveCake(cakes: MutableList<Cake>) {
        getOrCreateDataFile("picked.json").writeText(defaultJson.encodeToString(picked))
        getOrCreateDataFile("cakes.json").writeText(defaultJson.encodeToString(cakes))
    }

    @Trigger("handle")
    fun MessageEvent.match(): EventMatchResult? {
        if (Regex("^拿月饼\\s*(.*)").matches(message.content)) return EventMatchResult(Regex("^拿月饼\\s*(.*)").find(message.content))
        if (Regex("^查月饼").matches(message.content)) return EventMatchResult(Regex("^拿月饼\\s*(.*)").find(message.content),
            index = 1)
        return null
    }

    @kotlinx.serialization.Serializable
    data class Cake(
        val name: String,
        val emoji: String,
        val count: Int,
    )
}