package org.celery.command.controller

import events.ExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.warning
import org.celery.config.main.CallConfig
import org.celery.config.main.MainConfig.botOwner
import org.celery.config.main.MainConfig.superUsers
import org.celery.data.Coins
import org.celery.utils.sendMessage
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName

/**
 * @param priority 指令优先级,数字越大优先级越高
 */
@OptIn(ExperimentalStdlibApi::class)
abstract class EventCommand<E : BotEvent>(
    override val commandId: String,
    open val regex: Regex? = null,
    open val priority: Int = 5,
    open val normalUsage: String = "",
    open val description: String = "",
    open val example: String = "",
    open val configInfo: String = "",
    open val superUsage: String = "",
) : CCommand, CoroutineScope {
    open var blockSub = false
    open val logger = MiraiLogger.Factory.create(this::class)
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext + SupervisorJob()
    override val limitMessage: String? = null

    companion object {
        val limit = mutableListOf<Call>()
    }

    open fun E.setCoolDown(time: Int): Boolean {
        return false
    }

    enum class ExecutePermission {
        Console, SuperUser, Owner, Operator, AnyMember
    }

    enum class TriggerSubject {
        Friend, Group
    }

//    override var defaultEnable: Boolean = true
//    override var defaultBlockRunModeMode: BlockRunMode = BlockRunMode.Subject
//    override var defaultCallCountLimitMode: BlockRunMode = BlockRunMode.User
//    override var defaultCountLimit: Int
//        get() = PublicConfig[Keys.MAX_COUNT_DEFAULT, 20]
//        set(value) {
//            PublicConfig[Keys.MAX_COUNT_DEFAULT] = value
//        }
//
//    override var defaultCoolDown: Long
//        get() = PublicConfig[Keys.COOL_DOWN_DEFAULT, 3000L]
//        set(value) {
//            PublicConfig[Keys.COOL_DOWN_DEFAULT] = value
//        }
//    override var showTip: Boolean = true


    /**
     * defaultPermission: [ExecutePermission.AnyMember]
     */
    annotation class Command(
        vararg val executePermission: ExecutePermission = [ExecutePermission.AnyMember], val repeat: Int = 1,val coins:Int = 0
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
    suspend fun <E : BotEvent> matches(event: E): EventMatchResult? {
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
            val friendEvent = event.safeCast<FriendEvent>()
            if (friendEvent != null && !matcher.triggerSubject.contains(TriggerSubject.Friend)) return null
            if (event::class.isSubclassOf(it.extensionReceiverParameter!!.type.classifier.cast()).not()) {
                return null
            }
            val call =
                if (it.isSuspend)
                    it.callSuspend(this, event) ?: return null
                else
                    it.call(this, event) ?: return null
            if (event::class.isSubclassOf(kFunction.extensionReceiverParameter!!.type.classifier.cast()).not()) {
                logger.warning("该指令不能在当前聊天环境下执行")
                return null
            }
            return call as? EventMatchResult
        }
        error("no matcher for ${this.commandId}")
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

    open var blockGlobalAction: suspend E.() -> Any = {}
    open var blockSubjectAction: suspend E.() -> Any = {}
    open var blockUserAction: suspend E.() -> Any = {}
    open var blockPureUserAction: suspend E.() -> Any = {}

    /**
     * 通过反射拿到Command Matcher返回true时执行指令
     */
    suspend fun reactor(result: EventMatchResult, event: Event, call: Call): ExecutionResult {
        (getFunctions().ifEmpty {
            logger.warning { "指令${call.commandId}没有任何函数被标记为执行函数" }
            null
        } ?: return ExecutionResult.Error()).filterFunction(call)?.forEach {(it,anno)->
            if (it == null) return ExecutionResult.Success
            if (call.userId!=null&&anno.coins>Coins[call.userId]){
                if (event is MessageEvent){
                    event.sendMessage("他吗 你原石不够了！！至少要${anno.coins}个！")
                }
                return ExecutionResult.LimitCall
            }
            val type = event::class.starProjectedType
            val type1 = it.parameters.getOrNull(1)?.type
            val supertypeOf = type1?.isSupertypeOf(type)
            if (supertypeOf == true) {
                val functionReturn: Any? = if (it.parameters.size == 3) it.callSuspend(this, event, result)
                else it.callSuspend(this, event)
                var result: ExecutionResult = ExecutionResult.Unknown
                if (functionReturn.safeCast<ExecutionResult>() != null)
                    result = functionReturn
                else if (functionReturn.safeCast<Unit>() != null)
                    result = ExecutionResult.Success
                if (result==ExecutionResult.Success&&call.userId!=null&&anno.coins<Coins[call.userId]){
                    Coins[call.userId]-=anno.coins
                }
                return result
            }
            return ExecutionResult.Error(IllegalStateException("$type1 is not the isSupertypeOf of $type"))
        }
        return ExecutionResult.Error(IllegalStateException("no matching function"))
    }

    private fun List<Pair<KFunction<*>, Command>>.filterFunction(call: Call): List<Pair<KFunction<*>, Command>>? {
        val maxOf = maxOf { it.second.repeat }
        if (getCountLimit(call) < maxOf) {
            logger.warning { "指令的限制次数过小,无法执行第${maxOf}次重复调用时的内容" }
        }
        val i = getCallSize(call) + 1
        println("callSize = $i")
        var find: List<Pair<KFunction<*>, Command>>? = filter { it.second.repeat == i }

        if (find.isNullOrEmpty() && all { it.second.repeat <= i }) find =
            maxByOrNull { it.second.repeat }?.let { listOf(it) }

        return find
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

/**
 * @param normalUsage 用户使用时应该输入的指令前缀
 */
abstract class RegexCommand(
    override val commandId: String,
    override val regex: Regex,
    override val priority: Int = 5,
    override val normalUsage: String = "(触发正则表达式: ${regex.pattern})",
    override val description: String = "",
    override val example: String = "",
    override val configInfo: String = "",
    override val superUsage: String = "",
    open vararg val secondaryRegexs: Regex = arrayOf()
) : EventCommand<MessageEvent>(
    commandId,
    regex,
    priority,
    normalUsage,
    description,
    example,
    configInfo,
    superUsage
) {
    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = "$commandId: $normalUsage",
                params = listOf(),
                description = description,
                superUserUsage = "",
                example = example,
                commandId = commandId
            )
        )
    }

    override fun MessageEvent.setCoolDown(time: Int): Boolean {
        val call = Call(commandId, sender.id, subject.id)
        if (limit.any { it.commandId == call.commandId && it.userId == call.userId && it.subjectId == call.subjectId }) return true
        else {
            limit.add(call)
            launch {
                delay(time * 1000L)
                limit.remove(call)
            }
            return false
        }
    }

    val MessageEvent.isCoolDown: Boolean
        get() {
            val call = Call(commandId, sender.id, subject.id)
            return limit.any { it.commandId == call.commandId && it.userId == call.userId && it.subjectId == call.subjectId }
        }


    @Matcher
    open suspend fun MessageEvent.match(): EventMatchResult? {
        val primaryMatchResult = regex.find(message.content)?.let { EventMatchResult(it, 0) }
        if (primaryMatchResult != null) return primaryMatchResult
        var index = 1
        var subResult: EventMatchResult?
        for (regex in secondaryRegexs) {
            subResult = regex.find(message.content)?.let { EventMatchResult(it, index) }
            if (subResult != null) return subResult
            index++
        }
        return null
    }

}

/**
 * @param normalUsage 用户使用时应该输入的指令前缀
 */
abstract class MemberLeaveCommand(
    override val commandId: String,
    override val regex: Regex,
    override val priority: Int = 5,
    override val normalUsage: String = "(触发正则表达式: ${regex.pattern})",
    override val description: String = "",
    override val example: String = "",
    override val configInfo: String = "",
    override val superUsage: String = "",
    open vararg val secondaryRegexs: Regex = arrayOf()
) : EventCommand<MemberLeaveEvent>(
    commandId,
    regex,
    priority,
    normalUsage,
    description,
    example,
    configInfo,
    superUsage
) {
    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = "$commandId: $normalUsage",
                params = listOf(),
                description = description,
                superUserUsage = "",
                example = example,
                commandId = commandId
            )
        )
    }

    override fun MemberLeaveEvent.setCoolDown(time: Int): Boolean {
        val call = Call(commandId, member.id, groupId)
        if (limit.any { it.commandId == call.commandId && it.userId == call.userId && it.subjectId == call.subjectId }) return true
        else {
            limit.add(call)
            launch {
                delay(time * 1000L)
                limit.remove(call)
            }
            return false
        }
    }

    val MessageEvent.isCoolDown: Boolean
        get() {
            val call = Call(commandId, sender.id, subject.id)
            return limit.any { it.commandId == call.commandId && it.userId == call.userId && it.subjectId == call.subjectId }
        }
    override var showTip: Boolean
        get() = CallConfig["$commandId.show_tip", false]
        set(value) {
            CallConfig["$commandId.show_tip"]=value
        }

    @Matcher
    open suspend fun MemberLeaveCommand.match(): EventMatchResult? {
        return null
    }
}

private fun Contact.isSuperUser(): Boolean {
    return id == botOwner || (id in superUsers)
}

data class EventMatchResult(private val result: MatchResult?=null, private val index: Int = 0, val data: Any? = null) {
    operator fun get(index: Int) = result!!.groupValues[index]

    @JvmName("getResult1")
    fun getResult() = result ?: error("no match result there, it's not regex match command")
    inline fun <reified T : Any> getdata() = data.safeCast<T>()
        ?: error("no data there or cast to type:${T::class.qualifiedName ?: T::class.jvmName} faild.")

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

    override fun toString(): String {
        return "EventMatchResult(result=$result, index=$index, data=$data)"
    }

}