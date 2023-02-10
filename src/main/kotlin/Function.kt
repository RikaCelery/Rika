package org.celery

import net.mamoe.mirai.event.Event

/**
 *
 */
class Function<T : Event, E, R>(
    val matcher: Matcher<T, E>,
    private val _preHook: suspend (T.() -> Boolean) = { println("pre hook");true },
    private val _postHook:suspend (T.(R) -> Unit) = { println("post hook") },
    private val _exceptionProcessor:  (suspend T.(Throwable) -> Unit)? = null,
    private val _command: suspend T.(e: E) -> R,
) {
    companion object {
        val defaultHandler: (Event.(Throwable) -> Unit) = { exception ->
            throw IllegalStateException(
                "未找到异常处理器. 请实现 exceptionProcessor 方法", exception
            )
        }
    }


    val name: String = "unset"
    val priority: Int = 10
    val blockSub: Boolean = false

    suspend fun T.command(e: E): R = _command(e)
    suspend fun T.preHook(): Boolean = _preHook()
    suspend fun T.postHook(result: R): Unit = _postHook(result)
    suspend fun T.exceptionProcessor(e: Throwable): Unit? = _exceptionProcessor?.let { it(e) }
}
