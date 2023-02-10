package org.celery

import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.UserMessageEvent

object Test : Plugin("sd", "Test", builder = {
    newFunction(RegexMatcher(".*") { it is GroupMessageEvent }) {
        println("group only")
    }
    newFunction(RegexMatcher(".*") { it is UserMessageEvent }) {
        println("user only")
    }
    newFunction(RegexMatcher(".*") { it is UserMessageEvent || it is GroupMessageEvent }) {
        println("both group and user")
    }
}){
}
