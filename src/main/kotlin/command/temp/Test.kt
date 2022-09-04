package org.celery.command.temp

import com.celery.rika.utils.http.HttpUtils
import com.example.events.ExecutionResult
import org.celery.command.controller.RegexCommand
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import java.io.File
import java.lang.Exception

class Test : RegexCommand("testCommand", ".*我(.*)".toRegex()) {
    override var defultCountLimit: Int = 5

    @Command
    suspend fun MessageEvent.name(eventMatchResult: EventMatchResult): ExecutionResult {
        // PlainText
        // Image
        // FlashImage
        // FileMessage
        // ForwardMessage
        return ExecutionResult.Success()
    }
//    fun MessageEvent.handle(eventMatchResult: EventMatchResult){
//        Rika.logger.debug("event: $this")
//        Rika.logger.debug("matches: ${eventMatchResult.getResult().value}")
//    }
}