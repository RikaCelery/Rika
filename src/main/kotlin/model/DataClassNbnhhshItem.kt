package com.celery.rika.model

@kotlinx.serialization.Serializable
data class DataClassNbnhhshItem(
    val name: String,
    val inputting: List<String>? = null,
    val trans: List<String>? = null
) {
    val result: Pair<String, List<String>>
        get() = trans?.let { Pair(name, it) } ?: inputting?.let { Pair(name, it) } ?: Pair(name, listOf<String>())
}