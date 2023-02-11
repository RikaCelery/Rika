package org.celery

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.Event
import org.celery.Function.Companion.defaultHandler
import org.celery.Limiter.addCall
import org.celery.Limiter.checkCd
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions

open class Plugin(
    val id: String, val name: String, val priority: Int = 10, val builder: Builder.(p: Plugin) -> Unit =

        {}
) :
    CoroutineScope {

    class Builder(val p: Plugin) {
        fun build(p: Plugin) {
            p.functions.addAll(funs)
        }

        var name = ""
        var id = ""
        var priority = 10
        private val funs = mutableListOf<Function<*, *, *>>()
        fun id(id: String) {
            this.id = id
        }

        fun name(name: String) {
            this.name = name
        }

        fun priority(priority: Int) {
            this.priority = priority
        }

        fun <T : Event, E, R> newFunction(
            matcher: Matcher<T, E>,
            name: String = "${this.name}-subcommand",
            priority: Int = 10,
            _preHook: (suspend T.() -> Boolean) = { true },
            _postHook: suspend (T.(R) -> Unit) = { },
            _exceptionProcessor: (suspend T.(Throwable) -> Unit)? = null,
            _command: suspend T.(e: E) -> R,
        ) {
            funs.add(
                Function(
                    matcher, name, priority,
                    _preHooks = mutableListOf(_preHook),
                    _postHooks = mutableListOf(_postHook),
                    _exceptionProcessor = _exceptionProcessor,
                    _command = _command,
                    owner = p
                ).apply {
                    _preHooks.add { checkCd(this,"${p.name}.$name.p_call_limit",p,this@apply) }
                    _postHooks.add { addCall(this,"${p.name}.$name.p_call_limit",p,this@apply) }
                }
            )
        }
    }

    private val _coroutineContext = EmptyCoroutineContext
    final override val coroutineContext: CoroutineContext = _coroutineContext + SupervisorJob(_coroutineContext[Job])
    val functions: MutableList<Function<*, *, *>> = mutableListOf<Function<*, *, *>>()

    suspend fun call(e: Event): Int {
        val scop = CoroutineScope(this._coroutineContext)
        check(functions.isNotEmpty()) { "#${id}($name)至少需要一个子指令" }
        val runs = AtomicInteger(0)
        for (function in functions.sortedBy { it.priority }) {
            //防止类型不匹配的参数进入matcher
            if (!function.matcher.checkType(e)) continue
            //尝试匹配
            val result = function.matcher.callFunction("matches", e)
            if (result == null) {
                continue
            }
            //前置hook
            val preHook = function.callFunction("preHook", e) as Boolean
            if (!preHook) {
                Rika.logger.info("%s.%s前置检查未通过".format("#${id}($name)", function.name))
                continue
            }
            scop.launch {
                try {
                    val commandResult = function.callFunction("command", e, result)
                    //后置hook
                    function.callFunction("postHook", e, commandResult)
                    //如果发送异常,runs不会自增
                    runs.getAndIncrement()
                } catch (ex: Throwable) {
                    function.callFunction("exceptionProcessor", e, ex) ?: e.defaultHandler(ex)//为空使用默认异常处理程序
                }
            }
            //阻止低优先级的子指令继续执行
            if (function.blockSub) break
        }
        //返回被成功执行的指令数量
        return runs.get()
    }

    init {
        Builder(this).apply { builder(this@Plugin) }.build(this)
    }

}

private suspend fun Any.callFunction(name: String, vararg args: Any?): Any? {
    return this::class.functions.single {
        it.name == name
    }.callSuspend(this, *args)
}
