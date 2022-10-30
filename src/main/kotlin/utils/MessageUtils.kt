package org.celery.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import java.io.File
import java.io.InputStream

suspend fun MessageEvent.sendMessage(message: Message) = subject.sendMessage(message)

suspend fun MessageEvent.sendMessage(message: String) = subject.sendMessage(message)

suspend fun GroupMemberEvent.sendMessage(message: Message) = group.sendMessage(message)

suspend fun GroupMemberEvent.sendMessage(message: String) = group.sendMessage(message)

suspend fun File.toImage(subject: Contact): Image {
    return (toExternalResource().use {
        subject.uploadImage(it)
    })
}
suspend fun String.toImage(subject: Contact): Image {
    return SharedSelenium.render(this).toImage(subject)
}
suspend fun ByteArray.toImage(subject: Contact): Image {
    return (toExternalResource().use {
        subject.uploadImage(it)
    })
}

/**
 * 关流是调用者的责任
 */
suspend fun InputStream.toImage(subject: Contact): Image {
    return (toExternalResource().use {
        subject.uploadImage(it)
    })
}
suspend fun Contact.getAvatar(subject: Contact) = HttpUtils.downloader(this.avatarUrl).toImage(subject)