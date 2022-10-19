package org.celery.utils.permission

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.config.main.MainConfig


fun User?.isSuperUser(): Boolean {
    if (this == null)
        return true
    return MainConfig.superUsers.contains(id) || MainConfig.botOwner == id
}
/**
 * 检查调用者是否是群管理， 超级用户， console
 *
 */
fun CommandSender.isOperator(): Boolean {
    // 来自console则直接返回true
    val senderOnMessage = this as? CommandSenderOnMessage<*> ?: return true
    // 为空说明来自私聊
    val event = senderOnMessage.fromEvent as? GroupMessageEvent ?: return senderOnMessage.user.isSuperUser()
    if (event.sender.isOperator()) return true
    if (event.sender.isSuperUser()) return true
    return false
}

/**
 * ![CommandSender.isOperator]的便捷方法
 */
fun CommandSender.isNotOperator() = !isOperator()