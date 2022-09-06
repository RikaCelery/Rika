package org.celery.utils.interact

import kotlinx.coroutines.delay
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content

/**
 *  获取用户的下一条消息
 */
suspend fun CommandSenderOnMessage<*>.nextMessage(
    tip: String = "请输入一个参数",
    timeout: Int,
    TimeoutMsg: String? = null,
    autoRecall: Boolean = true,
    predicate: (MessageChain) -> Boolean = { true }
): MessageChain? {
    var set = false
    var msg: MessageChain? = null
    val logmsg = fromEvent.subject.sendMessage(tip)
    val sub = fromEvent.subject
    var n = 0
    val listener =
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            if ((sender == user) && (subject == sub) && predicate(message)) {
                msg = message
                set = true
            }
        }

    while (!set && n < timeout * 2) {
        delay(500)
        n++
    }
    listener.complete()
    if (autoRecall)
        try {
            logmsg.recall()
            delay(1000)
        } catch (e: Exception) {
            println("撤回失败$e")
        }
    if (n >= timeout * 2) {
        TimeoutMsg?.let { sub.sendMessage("$TimeoutMsg") }
        return null
    }
    return msg
}



suspend fun CommandSenderOnMessage<GroupMessageEvent>.nextOperatorMessage(
    tip: String = "请输入一个参数",
    timeout: Int,
    TimeoutMsg: String? = null,
    autoRecall: Boolean = true,
    predicate: (MessageChain) -> Boolean = { true }
): MessageChain? = nextMessage(tip, timeout, TimeoutMsg, autoRecall) {
    fromEvent.sender.isOperator()
}

suspend fun CommandSenderOnMessage<*>.getConfirm(
    tip: String,
    postFix: String = ",您确定吗[Y/N],请在60s内确认",
    timeout: Int = 60,
    TimeoutMsg: String? = null,
    autoRecall: Boolean = false,
): Boolean = nextMessage(
    tip + postFix,
    timeout,
    TimeoutMsg,
    autoRecall
) { it.content == "Y" || it.content == "y" || it.content == "N" || it.content == "n" }?.let { it.content == "Y" || it.content == "y" } == true