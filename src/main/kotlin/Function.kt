package org.celery

import net.mamoe.mirai.event.Event

/**
 *
 */
class Function<T : Event, E, R>(
    val matcher: Matcher<T, E>,
    val name: String = "subcommand",
    val priority: Int = 10,
    val blockSub: Boolean = false,
    val owner: Plugin,
    val _preHooks: MutableList<suspend (T.() -> Boolean)> = mutableListOf({
    println("pre hook for $name");true
    }),
    val _postHooks: MutableList<suspend (T.(R) -> Unit)> = mutableListOf({ println("post hook for $name") }),
    val _exceptionProcessor: (suspend T.(Throwable) -> Unit)? = null,
    private val _command: suspend T.(e: E) -> R,
) {
    companion object {
        val defaultHandler: (Event.(Throwable) -> Unit) = { exception ->
            throw IllegalStateException(
                "未找到异常处理器. 请实现 exceptionProcessor 方法", exception
            )
        }
    }

    val callLimits = mutableMapOf<String,PluginCallLimiter>() // 调用限制映射
    suspend fun T.command(e: E): R = _command(e)
    suspend fun T.preHook(): Boolean {
        return _preHooks.all { it(this) }
    }

    suspend fun T.postHook(result: R): Unit {
        _postHooks.onEach { it(result) }
    }

    suspend fun T.exceptionProcessor(e: Throwable): Unit? = _exceptionProcessor?.let { it(e) }
}
