package utils.time

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

private val Boolean.int: Int
    get() {
        return if (this) 1 else 0
    }
private val Int.boolean: Boolean
    get() {
        return this==1
    }

