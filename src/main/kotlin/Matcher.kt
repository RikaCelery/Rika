package org.celery

import net.mamoe.mirai.event.Event
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.isSubtypeOf

interface Matcher<T : Event, R> {
    /**
     * 判断事件[e]是否可以激活指令
     * @return [R]可被用于指令处理阶段,为空表示没有匹配到
     */
    suspend fun matches(e: T): R?
}

inline fun <T : Event, R, reified E : Event> Matcher<T, R>.checkType(e: E): Boolean {
    val matcherTargetClass =
        this::class.declaredFunctions.single { it.name == "matches" }
            .parameters[1].type
    val thisClass = e::class.defaultType
//    println("want: $matcherTargetClass")
//    println("actually: $thisClass")
    return thisClass.isSubtypeOf(matcherTargetClass)
}
