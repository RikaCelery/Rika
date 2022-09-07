package org.celery.command.controller

import events.ExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.FriendEvent
import net.mamoe.mirai.event.events.GroupMemberEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.full.*


@OptIn(ExperimentalStdlibApi::class)
abstract class EventCommand<E : BotEvent>(
    override val commandId: String,
    open val regex: Regex? = null,
    open val description: String = "",
    open val example: String = "",
    open val blockMode: CommandBlockMode = CommandBlockMode.BLACKLIST
) : CCommand, CoroutineScope {
    open val logger = MiraiLogger.Factory.create(this::class)
    override val coroutineContext: CoroutineContext = EmptyCoroutineContext + SupervisorJob()

    enum class ExecutePermission {
        Console, SuperUser, Owner, Operator, AnyMember
    }

    enum class TriggerSubject {
        Friend, Group
    }

    override var defultEnable: Boolean = true
    override var defultCallCountLimitMode: BlockRunMode = BlockRunMode.User
    override var defultCountLimit: Int = 10
    override var defultMinCooldown: Int = 5000
//    override var maxCall: Int = 10

    /**
     * defultPermission: [ExecutePermission.AnyMember]
     */
    annotation class Command(vararg val executePermission: ExecutePermission = arrayOf(ExecutePermission.AnyMember))

    /**
     * defultPermission: [TriggerSubject.Friend]+[TriggerSubject.Group]
     */
    annotation class Matcher(
        vararg val triggerSubject: TriggerSubject = arrayOf(
            TriggerSubject.Friend,
            TriggerSubject.Group
        )
    )

    /**
     * 通过反射拿到Matcher 调用Matcher判断是否触发指令
     */
    fun <E : BotEvent> matches(event: E): EventMatchResult? {
        this::class.functions.find {
            it.findAnnotations<Matcher>().isNotEmpty()
        }?.let {
            val groupMemberEvent = event.safeCast<GroupMemberEvent>()
            val matcher = it.findAnnotation<Matcher>()!!
            val command = this::class.functions.find { it.findAnnotations<Command>().singleOrNull() != null }!!
                .findAnnotation<Command>()!!
            if (groupMemberEvent != null)
                if (!matcher.triggerSubject.contains(TriggerSubject.Group))
                    return null
                else {
                    var result = false
                    for (perm in command.executePermission) {
                        when (perm) {
                            ExecutePermission.Console -> return null
                            ExecutePermission.SuperUser -> {
                                if (groupMemberEvent.member.isSuperUser())
                                    result = true
                            }
                            ExecutePermission.Owner -> {
                                if (groupMemberEvent.member.isOwner())
                                    result = true
                            }
                            ExecutePermission.Operator -> {
                                if (groupMemberEvent.member.isOperator())
                                    result = true
                            }
                            ExecutePermission.AnyMember -> {
                                result = true
                            }
                        }
                    }
                    if (!result)
                        return null
                }
            val friendEvent = event.safeCast<FriendEvent>()
            if (friendEvent != null && !matcher.triggerSubject.contains(TriggerSubject.Friend))
                return null
            val call = it.call(this, event, null)
            return call as? EventMatchResult
        }
        return null
    }


    /**
     * 通过反射拿到Command Matcher返回true时执行指令
     */
    suspend fun reactor(result: EventMatchResult, event: Event): ExecutionResult {
        this::class.functions.find {
            it.findAnnotations<Command>().isNotEmpty()
        }?.let {
            val type = event::class.starProjectedType
            val type1 = it.parameters.getOrNull(1)?.type
            val supertypeOf = type1?.isSupertypeOf(type)
            if (supertypeOf == true) {
                val call: Any? = if (it.parameters.size == 3)
                    it.callSuspend(this, event, result)
                else
                    it.callSuspend(this, event)
                return call.safeCast() ?: ExecutionResult.Unknown()
            }
            return ExecutionResult.Error(IllegalStateException("$type1 is not the isSupertypeOf oof $type"))
        } ?: this::class.functions.let {
            it.forEach { Rika.logger.error(it.name + it.annotations + it.parameters.joinToString { it.type.toString() }) }
            return ExecutionResult.Error(IllegalStateException("no matching function"))
        }
    }

    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = commandId + " (监听器)",
                params = listOf(),
                description = description,
                superUserUsage = "",
                example = example,
                commandId = commandId
            )
        )
    }

}

class EventMatchResult(private val result: MatchResult?,private val index:Int=0) {
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
    fun getIndexedResult(): Pair<Int, MatchResult?> {
        return index to result
    }
}

private fun Contact.isSuperUser(): Boolean {
    return true//FIXME:for test
}


/**
 * @param normalUsage 用户使用时应该输入的指令前缀
 */
abstract class RegexCommand(
    override val commandId: String,
    override val regex: Regex,
    open vararg val secondaryRegexs:Regex = arrayOf(),
    open val normalUsage: String = "(触发正则表达式: ${regex.pattern})",
    open val params: List<CommandUsage.CommandParam> = listOf(),
    override val description: String = "",
    override val example: String = "",
    override val blockMode: CommandBlockMode = CommandBlockMode.BLACKLIST
) : EventCommand<MessageEvent>(commandId, regex, description, example, blockMode) {
    override fun getUsages(): List<CommandBasicUsage> {
        return listOf(CommandBasicUsage(commandId + ": " + normalUsage, params, description, "", example, commandId))
    }

    @Matcher
    open suspend fun MessageEvent.match(): EventMatchResult? {
        val primaryMatchResult = regex.find(message.content)?.let { EventMatchResult(it,0) }
        if (primaryMatchResult!=null)
            return primaryMatchResult
        var index = 1
        var subResult: EventMatchResult?
        for (regex in secondaryRegexs){
            subResult = regex.find(message.content)?.let { EventMatchResult(it, index++) }
            if (subResult!=null)
                return subResult
        }
        return null
    }
}