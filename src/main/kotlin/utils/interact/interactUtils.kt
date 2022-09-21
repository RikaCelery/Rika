package org.celery.utils.interact

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage

/**
 *  获取用户下一条消息
 */
suspend fun MessageEvent.nextMessage(
    tip: String? = "请输入一个参数",
    timeout: Int,
    TimeoutMsg: String? = null,
    autoRecall: Boolean = true,
    predicate:suspend (MessageChain) -> Boolean = { true }
): MessageChain? {
    val logmsg = tip?.let { subject.sendMessage(it) }
    val sub = subject
    val msg: MessageChain? = try {
        nextMessage(timeout * 1000L) { predicate(it.message) }
    } catch (e:TimeoutCancellationException) {
        TimeoutMsg?.let { sub.sendMessage("$TimeoutMsg") }
        return null
    }
    if (autoRecall)
        try {
            logmsg?.recall()
            delay(1000)
        } catch (e: Exception) {
            println("撤回失败$e")
        }
    return msg
}



suspend fun GroupMessageEvent.nextOperatorMessage(
    tip: String? = "请输入一个参数",
    timeout: Int,
    TimeoutMsg: String? = null,
    autoRecall: Boolean = true,
    predicate: suspend (MessageChain) -> Boolean = { true }
): MessageChain? = nextMessage(tip, timeout, TimeoutMsg, autoRecall) {
    sender.isOperator()&&predicate(it)
}

suspend fun MessageEvent.getConfirm(
    tip: String,
    timeout: Int = 60,
    postFix: String = ",您确定吗[Y/N],请在${timeout}s内确认",
    TimeoutMsg: String? = null,
    autoRecall: Boolean = false,
): Boolean = nextMessage(
    tip + postFix,
    timeout,
    TimeoutMsg,
    autoRecall
) { it.content == "Y" || it.content == "y" || it.content == "N" || it.content == "n" }?.let { it.content == "Y" || it.content == "y" } == true