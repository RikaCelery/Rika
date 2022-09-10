package org.celery.command.controller

import events.ExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.warning
import org.celery.Rika
import org.celery.config.main.MainConfig
import org.celery.config.main.MainConfig.botOwner
import org.celery.config.main.MainConfig.superUsers
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

/**
 * @param priority 指令优先级,数字越大优先级越高
 */
@OptIn(ExperimentalStdlibApi::class)
abstract class EventCommand<E : BotEvent>(
    override val commandId: String,
    open val regex: Regex? = null,
    open val priority: Int = 5,
    open val description: String = "",
    open val example: String = "",
    open val blockMode: CommandBlockMode = CommandBlockMode.BLACKLIST
) : CCommand, CoroutineScope {
    open var blockSub = false
    open val logger = MiraiLogger.Factory.create(this::class)
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext + SupervisorJob()

    enum class ExecutePermission {
        Console, SuperUser, Owner, Operator, AnyMember
    }

    enum class TriggerSubject {
        Friend, Group
    }

    override var defaultEnable: Boolean = true
    override var defaultBlockRunModeMode: BlockRunMode = BlockRunMode.Subject
    override var defaultCallCountLimitMode: BlockRunMode = BlockRunMode.User
    override var defaultCountLimit: Int
        get() = MainConfig.defaultCountLimit
        set(value) {
            MainConfig.defaultCountLimit = value
        }

    override var defaultCoolDown: Long
        get() = MainConfig.defaultCoolDown
        set(value) {
            MainConfig.defaultCoolDown = value
        }
    override var showTip: Boolean = true


    /**
     * defaultPermission: [ExecutePermission.AnyMember]
     */
    annotation class Command(
        vararg val executePermission: ExecutePermission = [ExecutePermission.AnyMember], val repeat: Int = 1
    )

    /**
     * defaultPermission: [TriggerSubject.Friend]+[TriggerSubject.Group]
     */
    annotation class Matcher(
        vararg val triggerSubject: TriggerSubject = [TriggerSubject.Friend, TriggerSubject.Group]
    )

    /**
     * 通过反射拿到Matcher 调用Matcher判断是否触发指令
     */
    fun <E : BotEvent> matches(event: E): EventMatchResult? {
        this::class.functions.find {
            it.findAnnotations<Matcher>().isNotEmpty()
        }?.let { it ->
            val matcher = it.findAnnotation<Matcher>()!!
            val kFunction = this::class.functions.find { it.findAnnotations<Command>().singleOrNull() != null }!!
            val command = kFunction.findAnnotation<Command>()!!
            val groupMemberEvent = event.safeCast<GroupMemberEvent>()
            if (groupMemberEvent != null) if (!matcher.triggerSubject.contains(TriggerSubject.Group)) return null
            else {
                var result = false
                for (perm in command.executePermission) {
                    when (perm) {
                        ExecutePermission.Console -> return null
                        ExecutePermission.SuperUser -> {
                            if (groupMemberEvent.member.isSuperUser()) result = true
                        }
                        ExecutePermission.Owner -> {
                            if (groupMemberEvent.member.isOwner() || groupMemberEvent.member.isSuperUser()) result =
                                true
                        }
                        ExecutePermission.Operator -> {
                            if (groupMemberEvent.member.isOperator() || groupMemberEvent.member.isSuperUser()) result =
                                true
                        }
                        ExecutePermission.AnyMember -> {
                            result = true
                        }
                    }
                }
                if (!result) return null
            }
            val friendEvent = event.safeCast<GroupMemberEvent>()
            if (friendEvent != null && !matcher.triggerSubject.contains(TriggerSubject.Friend)) return null
            val call = it.call(this, event, null) ?: return null
            if (event::class.isSubclassOf(kFunction.extensionReceiverParameter!!.type.classifier.cast()).not()) {
                logger.warning("该指令不能在当前聊天环境下执行")
                return null
            }
            return call as? EventMatchResult
        }
        return null
    }

    fun hasPermission(subject: Contact?, user: User?): Boolean {
        subject ?: return true
        user ?: return true
        this::class.functions.find {
            it.findAnnotations<Matcher>().isNotEmpty()
        }?.let { it ->
            val matcher = it.findAnnotation<Matcher>()!!
            val command = this::class.functions.find { it.findAnnotations<Command>().singleOrNull() != null }!!
                .findAnnotation<Command>()!!
            if (subject.id != user.id) if (!matcher.triggerSubject.contains(TriggerSubject.Group)) return false
            else {
                user.cast<NormalMember>()
                var result = false
                for (perm in command.executePermission) {
                    when (perm) {
                        ExecutePermission.Console -> return false
                        ExecutePermission.SuperUser -> {
                            if (user.isSuperUser()) result = true
                        }
                        ExecutePermission.Owner -> {
                            if (user.isOwner() || user.isSuperUser()) result = true
                        }
                        ExecutePermission.Operator -> {
                            if (user.isOperator() || user.isSuperUser()) result = true
                        }
                        ExecutePermission.AnyMember -> {
                            result = true
                        }
                    }
                }
                if (!result) return false
            }
            if (!matcher.triggerSubject.contains(TriggerSubject.Friend)) return false
            return true
        }
        return false
    }

    /**
     * 通过反射拿到Command Matcher返回true时执行指令
     */
    suspend fun reactor(result: EventMatchResult, event: Event, call: Call): ExecutionResult {
        (getFunctions().ifEmpty {
            logger.warning { "指令${call.commandId}没有任何函数被标记为执行函数" }
            null
        } ?: return ExecutionResult.Error()).filterFunction(call).let {
            if (it == null) return ExecutionResult.Success
            val type = event::class.starProjectedType
            val type1 = it.parameters.getOrNull(1)?.type
            val supertypeOf = type1?.isSupertypeOf(type)
            if (supertypeOf == true) {
                val functionReturn: Any? = if (it.parameters.size == 3) it.callSuspend(this, event, result)
                else it.callSuspend(this, event)
                if (functionReturn.safeCast<ExecutionResult>() != null) return functionReturn
                else if (functionReturn.safeCast<Unit>() != null) return ExecutionResult.Success
                return ExecutionResult.Unknown
            }
            return ExecutionResult.Error(IllegalStateException("$type1 is not the isSupertypeOf of $type"))
        } ?: this::class.functions.let { it ->
            it.forEach { it -> Rika.logger.error(it.name + it.annotations + it.parameters.joinToString { it.type.toString() }) }
        }
        return ExecutionResult.Error(IllegalStateException("no matching function"))
    }
//    val callSizeGetMode =
    private fun List<Pair<KFunction<*>, Command>>.filterFunction(call: Call): KFunction<*>? {
        val maxOf = maxOf { it.second.repeat }
        if (getCountLimit(call) < maxOf) {
            logger.warning { "指令的限制次数过小,无法执行第${maxOf}次重复调用时的内容" }
        }
        val i = getCallSize(call)+1
        println("callSize = $i")
        var find = find { it.second.repeat == i }
        if (find == null && all { it.second.repeat <= i }) find = maxByOrNull { it.second.repeat }
        return find?.first
    }

    private fun getFunctions() = this::class.functions.mapNotNull {
        if (it.findAnnotation<Command>() != null) it to it.findAnnotation<Command>()!!
        else null
    }

    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = "$commandId (监听器)",
                params = listOf(),
                description = description,
                superUserUsage = "",
                example = example,
                commandId = commandId
            )
        )
    }

}

class EventMatchResult(private val result: MatchResult?, private val index: Int = 0) {
    operator fun get(index: Int) = result!!.groupValues[index]

    @JvmName("getResult1")
    fun getResult() = result ?: error("no match result there, it's not regex match command")
    fun getAllMatches(): MutableList<String> {
        val list = mutableListOf<String>()
        var last: MatchResult? = getResult()
        while (last != null) {
            list.add(last.value)
            last = last.next()
        }
        return list
    }

    fun getIndexedResult(): Pair<Int, MatchResult> {
        return index to result!!
    }
}

private fun Contact.isSuperUser(): Boolean {
    return id == botOwner || (id in superUsers)
}


/**
 * @param normalUsage 用户使用时应该输入的指令前缀
 */
abstract class RegexCommand(
    override val commandId: String,
    override val regex: Regex,
    override val priority: Int = 5,
    open val normalUsage: String = "(触发正则表达式: ${regex.pattern})",
    open val params: List<CommandUsage.CommandParam> = listOf(),
    override val description: String = "",
    override val example: String = "",
    open val configInfo: String = "",
    override val blockMode: CommandBlockMode = CommandBlockMode.BLACKLIST,
    open vararg val secondaryRegexs: Regex = arrayOf()
) : EventCommand<MessageEvent>(commandId, regex, priority, description, example, blockMode) {
    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = "$commandId: $normalUsage",
                params = params,
                description = description,
                superUserUsage = "",
                example = example,
                commandId = commandId
            )
        )
    }

    override var showTip: Boolean = true

    @Matcher
    open suspend fun MessageEvent.match(): EventMatchResult? {
        val primaryMatchResult = regex.find(message.content)?.let { EventMatchResult(it, 0) }
        if (primaryMatchResult != null) return primaryMatchResult
        var index = 1
        var subResult: EventMatchResult?
        for (regex in secondaryRegexs) {
            subResult = regex.find(message.content)?.let { EventMatchResult(it, index++) }
            if (subResult != null) return subResult
        }
        return null
    }
}