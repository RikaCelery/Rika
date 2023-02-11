package org.celery

object Test : Plugin("sd", "Test", builder = {
    newFunction(RegexMatcher("^(.)\\1+$"),name = "复读", _postHook = {Rika.logger.info("复读$it")}) {
        subject.sendMessage(it.groupValues[1])
        it.groupValues[1]
    }
})
