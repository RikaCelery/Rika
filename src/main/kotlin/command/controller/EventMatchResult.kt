package org.celery.command.controller

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class EventMatchResult(
    private val matchResult: MatchResult?=null,
    val index:Int=0,
    val data:Any?=null
) {
    fun getResult(): MatchResult {
        return matchResult!!
    }
    operator fun get(index: Int): String {
        return getResult().groupValues[index]
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <reified T> getdata(): T {
        contract {
            returns() implies (this@EventMatchResult is T)
        }
        return data as T
    }

    fun getIndexedResult(): Pair<Int, MatchResult> {
        return index to matchResult!!
    }

}
