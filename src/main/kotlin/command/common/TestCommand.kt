package org.celery.command.common

import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage

object TestCommand:RegexCommand("test","^test$".toRegex()) {
    @Command
    suspend fun MessageEvent.handle1(){
        sendMessage("第1次调用")
    }

    @Command(repeat = 5)
    suspend fun MessageEvent.handle5(){
        sendMessage("第5次调用")
    }
    @Command(repeat = 6)
    suspend fun MessageEvent.handle6(){
        sendMessage("第6或以上次调用")
    }
}