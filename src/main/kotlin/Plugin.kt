package org.celery

import kotlinx.coroutines.*
import net.mamoe.mirai.event.Event
import org.celery.Function.Companion.defaultHandler
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.memberFunctions

open class Plugin(
    val id: String,
    val name: String,
    val priority: Int = 10,
    val builder: Builder.() -> Unit={}
) : CoroutineScope {

    class Builder {
        fun build(p:Plugin){
            p.functions.addAll(funs)
        }
        var name = ""
        var id = ""
        var priority = 10
        private val funs = mutableListOf<Function<*, *, *>>()
        fun id(id: String){
            this.id = id
        }
        fun name(name: String){
            this.name = name
        }
        fun priority(priority: Int){
            this.priority = priority
        }
        fun <T : Event,E, R>newFunction(
            matcher: org.celery.Matcher<T, E>,
            _preHook: suspend (T.() -> Boolean) = { true },
            _postHook: suspend (T.(R) -> Unit) = {  },
            _exceptionProcessor: (suspend T.(Throwable) -> Unit)? = null,
            _command: suspend T.(e: E) -> R,
        ) {
            funs.add(Function(matcher, _preHook, _postHook, _exceptionProcessor, _command))
        }
    }

    private val _coroutineContext = EmptyCoroutineContext
    override val coroutineContext: CoroutineContext = _coroutineContext + SupervisorJob(_coroutineContext[Job])
    val functions = mutableListOf<Function<*, *, *>>()

    //    private suspend fun KClass<*>.suspendCall(vararg any: Any?): Any? {
//        return (this as KFunction<*>).callSuspend(any)
//    }
    private fun KClass<*>.call(vararg any: Any?): Any? {
        val function = this.java
        return function.methods.first { it.name == "invoke" }.let {
            println(it.name + it.genericReturnType + it.genericParameterTypes.joinToString())
//        if (it.genericReturnType.typeName=="java.lang.Boolean")
            it.invoke(function.newInstance(), any)
        }
    }

    suspend fun call(e: Event) = coroutineScope {
        check(functions.isNotEmpty()) { "#${id}($name)至少需要一个子指令" }
        val runs = AtomicInteger(0)
        for (function in functions.sortedBy { it.priority }) {
            //防止类型不匹配的参数进入matcher
            if (!function.matcher.checkType(e)) continue
            //尝试匹配
            val result = function.matcher::class.memberFunctions.single { it.name == "matches" }.callSuspend(
                function.matcher, e
            ) ?: continue
            //前置hook
            val preHook = function::class.declaredMemberExtensionFunctions.single {
                it.name == "preHook"
            }.callSuspend(function, e) as Boolean

            if (!preHook) {
                Rika.logger.info("%s.%s前置检查未通过".format("#${id}($name)", function.name))
                continue
            }
            launch {
                try {
                    with(function) {
                        val commandResult = function::class.declaredMemberExtensionFunctions.single {
                            it.name == "command"
                        }.callSuspend(function, e, result)
                        //后置hook
                        function::class.declaredMemberExtensionFunctions.single {
                            it.name == "postHook"
                        }.callSuspend(function, e, commandResult)

                        //如果发送异常,runs不会自增
                        runs.getAndIncrement()
                    }
                } catch (ex: Throwable) {

                    function::class.declaredMemberExtensionFunctions.single {
                        it.name == "exceptionProcessor"
                    }.callSuspend(function, e, ex) ?: e.defaultHandler(ex)//为空使用默认异常处理程序
                }
            }
            //阻止低优先级的子指令继续执行
            if (function.blockSub) break
        }
        //返回被成功执行的指令数量
        return@coroutineScope runs.get()
    }

    @Target(AnnotationTarget.FUNCTION)
    annotation class Matcher(val target: String)

    @Target(AnnotationTarget.FUNCTION)
    annotation class Command()

    init {
        Builder().apply(builder).build(this)
    }
}
