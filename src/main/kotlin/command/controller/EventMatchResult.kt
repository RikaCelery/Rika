package org.celery.command.controller

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.jvm.jvmName

class EventMatchResult(
    private val matchResult: MatchResult?=null,
    val index:Int=0,
    val data:Any?=null
) {

    fun getAllMatches(): MutableList<String> {
        val list = mutableListOf<String>()
        var last: MatchResult? = getResult()
        while (last != null) {
            list.add(last.value)
            last = last.next()
        }
        if (list.isEmpty())
            error("no match here")
        return list
    }


    override fun toString(): String {
        return "EventMatchResult(result=$matchResult, index=$index, data=$data)"
    }
    fun getResult(): MatchResult {
        return matchResult ?: error("no match result here, it's not regex match command")
    }
    operator fun get(index: Int): String {
        return getResult().groupValues[index]
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <reified T> getdata(): T {
        contract {
            returns() implies (this@EventMatchResult is T)
        }
        return (data as? T) ?: error("no data there or cast to type:${T::class.qualifiedName ?: T::class.jvmName} faild.")
    }

    fun getIndexedResult(): Pair<Int, MatchResult> {
        return index to( matchResult ?: error("no match result there, it's not regex match command"))
    }

}
