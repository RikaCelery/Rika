package events

import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.BotEvent
import org.celery.command.controller.Call
import org.celery.command.controller.EventCommand
import org.celery.command.controller.EventMatchResult

interface CommandExecutionEvent<E : Event> : Event {
    val eventCommand: EventCommand<*>
    val call: Call
    val fromEvent: E
    val matches: EventMatchResult?

    //    val ignoreFrequencyCheck:Boolean
//    val ignoreCountCheck:Boolean
    val reactor: () -> ExecutionResult
}

class EventCommandExecutionEvent<E : BotEvent>(
    override val eventCommand: EventCommand<*>,
    override val fromEvent: E,
    override val matches: EventMatchResult?,
    override val call: Call,
    override val reactor: () -> ExecutionResult
) : CommandExecutionEvent<E>, AbstractEvent()

sealed class ExecutionResult {
    object Success : ExecutionResult()

    /**
     * 指令没有正常执行，但需要限制调用频率
     */
    object LimitCall: ExecutionResult()
    class Failed(val exception: Throwable? = null, val message:String? = null) : ExecutionResult(){
        constructor(exception: Throwable): this(exception, null)
    }
    class Ignored(val reason: String? = null) : ExecutionResult(){
        companion object NoReason:ExecutionResult()
    }
    object Unknown : ExecutionResult()
    class Error(val cause: Throwable? = null) : ExecutionResult()
}