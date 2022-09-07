package org.celery.command.controller

import events.CommandExecutionEvent
import events.EventCommandExecutionEvent
import events.ExecutionResult
import events.ExecutionResult.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.safeCast
import org.celery.Rika
import org.celery.command.controller.BlockRunMode.*
import kotlin.coroutines.CoroutineContext

/*
event -> findMatch( Command.matches(event)!=null ) -> ExecutionEvent -> execute

 */
/**
 * 指令执行器
 */
object CommandExecuter : SimpleListenerHost() {
    private val logger = Rika.logger
    private val commands = hashSetOf<EventCommand<*>>()
    fun add(c: EventCommand<*>) = commands.add(c)

    /**
     * 执行指令
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    suspend fun CommandExecutionEvent<Event>.handle() {
        val result: ExecutionResult = when (eventCommand.getBlockRunMode(call.commandId)) {
            Global -> with(Limitable) {
                if (blockGlobal[call.commandId] == true) {
                    Ignored("blockGlobal blocked.")
                } else {
                    blockGlobal[call.commandId] = true
                    val executionResult = try {
                        reactor()
                    } catch (e: Exception) {
                        Faild(e)
                    } finally {
                        blockGlobal[call.commandId] = false
                    }
                    executionResult
                }
            }
            Subject -> with(Limitable) {
                val commandName = call.commandId
                if (blockSubject[commandName]?.contains(call.subjectId) == true) {
                    Ignored("blockSubject blocked.")
                } else {
                    blockSubject[commandName]?.add(call.subjectId) ?: blockSubject.put(
                        commandName,
                        mutableListOf(call.subjectId)
                    )
                    val executionResult = try {
                        reactor()
                    } catch (e: Exception) {
                        Faild(e)
                    } finally {
                        blockSubject[commandName]!!.remove(call.subjectId)
                    }
                    executionResult
                }
            }
            User -> with(Limitable) {
                if (blockUser[call.commandId]?.contains(call.userId) == true) {
                    Ignored("blockUser blocked.")
                } else {
                    blockUser[call.commandId]?.add(call.userId) ?: blockUser.put(
                        call.commandId,
                        mutableListOf(call.userId)
                    )
                    val executionResult = try {
                        reactor()
                    } catch (e: Exception) {
                        Faild(e)
                    } finally {
                        blockUser[call.commandId]!!.remove(call.userId)
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
            is Faild -> {
                fromEvent.safeCast<GroupEvent>()?.group?.sendMessage("在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${result.exception?.message?.let { if (it.length>30) it.substring(0..30) else it }}")
                fromEvent.safeCast<MessageEvent>()?.subject?.sendMessage("在执行指令时出现了无法处理的错误${result.exception?.javaClass?.simpleName}, ${result.exception?.message?.let { if (it.length>30) it.substring(0..30) else it }}")
                logger.error("exception happened when executing $call caused by ${result.exception}.${result.exception?.stackTraceToString()?.let { "\n$it" }}")
            }
            is Ignored -> {
                logger.debug("ignore reason: ${result.reason}.")
            }
            is Unknown -> {
                logger.warning("unkown status, not add.")
            }
            is Error -> { fromEvent.safeCast<GroupEvent>()?.group?.sendMessage("在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${result.cause?.message?.let { if (it.length>30) it.substring(0..30) else it }}")
                fromEvent.safeCast<MessageEvent>()?.subject?.sendMessage("在执行指令时出现了一个内部错误${result.cause?.javaClass?.simpleName}, ${result.cause?.message?.let { if (it.length>30) it.substring(0..30) else it }}")

                logger.error("internal error happened when executing $call caused by ${result.cause}.${result.cause?.stackTraceToString()?.let { "\n$it" }}")
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
        for (cmd in commands) {
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
                            cmd.reactor(matches, this@listen)
                        }
                    }.broadcast()
                }
                logger.debug("broadcast success")
            }
        }
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        exception.printStackTrace()
    }
}