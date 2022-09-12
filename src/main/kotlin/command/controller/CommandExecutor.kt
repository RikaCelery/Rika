package org.celery.command.controller

import events.CommandExecutionEvent
import events.EventCommandExecutionEvent
import events.ExecutionResult
import events.ExecutionResult.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.findIsInstance
import net.mamoe.mirai.utils.safeCast
import org.celery.Rika
import org.celery.command.controller.BlockRunMode.*
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.CoroutineContext

/*
event -> findMatch( Command.matches(event)!=null ) -> ExecutionEvent -> execute

 */
/**
 * 指令执行器
 */
object CommandExecutor : SimpleListenerHost() {
    private val logger = Rika.logger
    private val commands = hashSetOf<EventCommand<*>>()
    fun add(c: EventCommand<*>) = commands.add(c)

    val lastMessages = ConcurrentLinkedDeque<MessageEvent>()

    /**
     * 执行指令
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    suspend fun CommandExecutionEvent<Event>.handle() {
        val result: ExecutionResult = when (eventCommand.getBlockRunMode(call.commandId)) {
            Global -> with(Limitable) {
                if (blockGlobal[call.commandId] == true) {
                    eventCommand.safeCast<RegexCommand>()?.blockGlobalAction?.let {
                        fromEvent.safeCast<MessageEvent>()?.let { it1 -> it(it1) }
                    }

                    Ignored("blockGlobal blocked.")
                } else {
                    blockGlobal[call.commandId] = true
                    val executionResult = try {
                        reactor()
                    } catch (e: InvocationTargetException) {
                        Failed(e.cause, e.cause?.message)
                    } catch (e: Exception) {
                        Failed(exception = e, message = e.message)
                    } finally {
                        blockGlobal[call.commandId] = false
                    }
                    executionResult
                }
            }
            Subject -> with(Limitable) {
                val commandName = call.commandId
                if (blockSubject[commandName]?.contains(call.subjectId) == true) {
                    eventCommand.safeCast<RegexCommand>()?.blockSubjectAction?.let {
                        fromEvent.safeCast<MessageEvent>()?.let { it1 -> it(it1) }
                    }
                    Ignored("blockSubject blocked.")
                } else {
                    blockSubject[commandName]?.add(call.subjectId) ?: blockSubject.put(
                        commandName,
                        mutableListOf(call.subjectId)
                    )
                    val executionResult = try {
                        reactor()
                    } catch (e: InvocationTargetException) {
                        Failed(e.cause, e.cause?.message)
                    } catch (e: Exception) {
                        Failed(exception = e, message = e.message)
                    } finally {
                        blockSubject[call.commandId]?.remove(call.subjectId)
                    }
                    executionResult
                }
            }
            User -> with(Limitable) {
                if (blockUser[call.commandId]?.contains(call.userId) == true) {
                    eventCommand.safeCast<RegexCommand>()?.blockUserAction?.let {
                        fromEvent.safeCast<MessageEvent>()?.let { it1 -> it(it1) }
                    }
                    Ignored("blockUser blocked.")
                } else {
                    blockUser[call.commandId]?.add(call.userId) ?: blockUser.put(
                        call.commandId,
                        mutableListOf(call.userId)
                    )
                    val executionResult = try {
                        reactor()
                    } catch (e: InvocationTargetException) {
                        Failed(e.cause, e.cause?.message)
                    } catch (e: Exception) {
                        Failed(exception = e, message = e.message)
                    } finally {
                        blockUser[call.commandId]?.remove(call.userId)
                    }
                    executionResult

                }
            }
            PureUser -> with(Limitable) {
                if (blockUser[call.commandId]?.contains(call.userId) == true) {
                    eventCommand.safeCast<RegexCommand>()?.blockPureUserAction?.let {
                        fromEvent.safeCast<MessageEvent>()?.let { it1 -> it(it1) }
                    }
                    Ignored("blockPureUser blocked.")
                } else {
                    blockUser[call.commandId]?.add(call.userId) ?: blockUser.put(
                        call.commandId,
                        mutableListOf(call.userId)
                    )
                    val executionResult = try {
                        reactor()
                    } catch (e: InvocationTargetException) {
                        Failed(e.cause, e.cause?.message)
                    } catch (e: Exception) {
                        Failed(exception = e, message = e.message)
                    } finally {
                        blockUser[call.commandId]?.remove(call.userId)
                    }
                    executionResult

                }
            }
        }

        when (result) {
            is Success -> {
                logger.debug("executed successfully.")
                eventCommand.addCall(call)
            }
            is Failed -> {
                fromEvent.safeCast<GroupEvent>()?.group?.sendMessage(
                    "在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${
                        result.exception?.message ?: result.message?.let {
                            if (it.length > 100) it.substring(
                                0..100
                            ) else it
                        }
                    }"
                )
                    ?: fromEvent.safeCast<MessageEvent>()?.subject?.sendMessage(
                        "在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${
                            result.exception?.message ?: result.message?.let {
                                if (it.length > 100) it.substring(
                                    0..100
                                ) else it
                            }
                        }"
                    )
                logger.error(
                    "exception happened when executing $call caused by ${result.exception}.${
                        result.exception?.stackTraceToString()?.let { "\n$it" }
                    }"
                )
            }
            is Ignored -> {
                logger.debug("ignore reason: ${result.reason}.")
            }
            is Unknown -> {
                logger.warning("unknown status, not add.")
            }
            is Error -> {
                fromEvent.safeCast<GroupEvent>()?.group?.sendMessage(
                    "在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${
                        result.cause?.message?.let {
                            if (it.length > 100) it.substring(
                                0..100
                            ) else it
                        }
                    }"
                )
                    ?: fromEvent.safeCast<MessageEvent>()?.subject?.sendMessage(
                        "在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${
                            result.cause?.message?.let {
                                if (it.length > 100) it.substring(
                                    0..100
                                ) else it
                            }
                        }"
                    )

                logger.error(
                    "internal error happened when executing $call caused by ${result.cause}.${
                        result.cause?.stackTraceToString()?.let { "\n$it" }
                    }"
                )
            }
            is LimitCall -> {
                eventCommand.addCall(call)
                launch {
                    delay(eventCommand.getCoolDown(eventCommand.commandId).toLong())
                    eventCommand.removeCall(call)
                }
            }
        }
    }


    /**
     * 从消息/事件中识别指令
     *
     * 原事件会被包装到CommandExecutionEvent中
     */
    @EventHandler
    suspend fun MessageEvent.listen() {
        val filter = lastMessages.filter { it.sender == sender && it.subject == subject }
        if (filter.size >= 2) {
            lastMessages.remove(filter.first())
        }
        lastMessages.add(this)
        for (cmd in commands.sortedByDescending { it.priority }) {
//            println(cmd.commandId)
            val matches = cmd.matches(this@listen)
            if (matches != null) {
                logger.debug("find match: ${cmd.commandId}")
                val call = Call(cmd.commandId, sender.id, subject.id)
                if (!cmd.canCall(call))
                    continue
                launch {
                    EventCommandExecutionEvent(
                        eventCommand = cmd,
                        fromEvent = this@listen,
                        matches = matches,
                        call = call
                    ) {
                        logger.debug("executing...")
                        runBlocking {
                            cmd.reactor(matches, this@listen, call)
                        }
                    }.broadcast()
                }
                logger.debug("broadcast success")
                if (cmd.blockSub)
                    break
            }
        }
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        exception.printStackTrace()
    }


    inline fun <reified M : SingleMessage?> lastInstanceOrNull(messageEvent: MessageEvent): M? =
        lastMessages.reversed().find { it.sender==messageEvent.sender&&it.subject==messageEvent.subject&&it.message.findIsInstance<M>()!=null }?.message?.findIsInstance<M>()


}