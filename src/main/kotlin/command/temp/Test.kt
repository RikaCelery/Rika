package org.celery.command.temp

import org.celery.command.controller.RegexCommand
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult

class Test: RegexCommand(
    "testCommand",
    ".{5,1000}".toRegex()
) {
    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult){
        Rika.logger.debug("event: $this")
        Rika.logger.debug("matches: ${eventMatchResult.getResult().value}")
    }
}