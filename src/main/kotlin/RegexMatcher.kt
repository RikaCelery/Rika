package org.celery

import net.mamoe.mirai.event.events.MessageEvent
import org.intellij.lang.annotations.Language

class RegexMatcher(
    private val regex: Regex,
    private val matchType: MatchType = MatchType.PLAIN,
    val messageFilter: (MessageEvent) -> Boolean = { true }
) : Matcher<MessageEvent, MatchResult> {
    enum class MatchType {
        MIRAI_CODE, PLAIN
    }

    constructor(
        @Language("RegExp") pattern: String,
        matchType: MatchType = MatchType.PLAIN,
        messageFilter: (MessageEvent) -> Boolean = { true }
    ) : this(
        Regex(pattern, RegexOption.MULTILINE), matchType, messageFilter
    )

    override suspend fun matches(e: MessageEvent): MatchResult? {
        if (!messageFilter(e)) {
            return null
        }
        return if (matchType == MatchType.PLAIN) regex.matchEntire(e.message.contentToString())
        else regex.matchEntire(e.message.serializeToMiraiCode())
    }
}
