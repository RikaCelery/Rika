package org.celery.utils

import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Message

suspend fun MessageEvent.sendMessage(message: Message) = subject.sendMessage(message)

suspend fun MessageEvent.sendMessage(message: String) = subject.sendMessage(message)