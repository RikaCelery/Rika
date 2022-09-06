package org.celery.command.temp

import events.ExecutionResult
import org.celery.command.controller.RegexCommand
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.EventMatchResult

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