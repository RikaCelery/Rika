package org.celery.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File

suspend fun MessageEvent.sendMessage(message: Message) = subject.sendMessage(message)

suspend fun MessageEvent.sendMessage(message: String) = subject.sendMessage(message)

suspend fun File.toImage(subject: Contact): Image {
    return (toExternalResource().use {
        subject.uploadImage(it)
    })
}