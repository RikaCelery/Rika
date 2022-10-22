package org.celery.command.controller.abs

import events.ExecutionResult
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.cast
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.safeCast
import org.apache.commons.text.StringSubstitutor
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.AbstractCommand.RequirePermission.*
import org.celery.config.Reloadable
import org.celery.config.main.MainConfig
import org.celery.data.Coins
import org.celery.utils.file.createParentFolder
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*


/**
 *
 */
@Suppress("unused")
abstract class AbstractCommand(
    override val commandId: String,
    /**
     * higher means higher priority
     */
    val priority: Int = 5,
    val classify: String = "default",
    val usage: String = "",
    val superUserUsage: String = "",
    val example: String = "",
) : NEWLimitable {
    open val blockSub: Boolean = false
    val limitPerDay: Int
        get() {
            TODO()
        }
    protected val logger by lazy {
        MiraiLogger.Factory.create(this::class,"Rika-$commandId")
    }
    val config by lazy {
        object : Reloadable(Rika.configFolderPath.resolve("plugin-configs").resolve(commandId).toString()) {

        }
    }
    val data by lazy {
        object : Reloadable(Rika.dataFolderPath.resolve("plugin-datas").resolve(commandId).toString()) {

        }
    }

    protected annotation class Trigger0
    protected annotation class Runner
    annotation class Trigger(val targetFunctionName: String)
    annotation class Command(
        val regex: String = "",
        val coin: Int = 0,
        val identity: String = "subCommand",
        val description: String = "",
        val permissions: Array<RequirePermission> = [],
    )

    enum class RequirePermission {
        /* 限定指令执行环境 */
        /**
         * 好友或群员(非匿名)均可
         */
        ANY, FRIEND, MEMBER, ONLY_FRIEND, ONLY_MEMBER,

        /* 限定指令者权限 */
        OPERATOR, OWNER, SUPERUSER
    }

    open suspend fun checkPermission(event: Event): Boolean {
        return false
    }

    abstract suspend fun triggered(event: Event): Boolean
    suspend fun runCommand(event: Event): Pair<ExecutionResult, Int> {
        val let = this::class.functions.find { it.hasAnnotation<Runner>() }?.let {
            return@let try {
                if (it.isSuspend) it.callSuspend(this, event)
                else it.call(this, event)
            } catch (e: Exception) {
                throw e
            }
        }
        return (let as? Pair<ExecutionResult, Int>) ?: (ExecutionResult.Unknown to 0)
    }


    /**
     * 拿一个文件
     */
    fun getResource(relative: String): File {
        val file = Rika.dataFolderPath.resolve("resources").resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getOrCreateResource(relative: String): File {
        val file = Rika.dataFolderPath.resolve("resources").resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not()) file.createNewFile()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("plugin-datas").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("plugin-datas").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not()) file.createNewFile()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_" + parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_" + parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not()) file.createNewFile()
        return file
    }


    protected fun Event.checkTarget(permission: RequirePermission): Boolean {
        if (permission == ANY) return true
        val isFriend = this is FriendEvent
        val isGroup = this is GroupEvent
        if (permission == ONLY_FRIEND && isGroup) return false
        if (permission == ONLY_MEMBER && isFriend) return false
        if (permission == FRIEND && isFriend) return true
        if (permission == MEMBER && isGroup) return true
        return false
    }

    protected fun Event.checkTargetPermission(permission: RequirePermission): Boolean {
        println("this::class.qualifiedName = ${this::class.qualifiedName}")
        return when (permission) {
            OPERATOR -> {
                val b = this.safeCast<GroupMemberEvent>()?.member?.let { it.isOperator() || it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<GroupMemberEvent>().member.let {
                        "member:${it.id} is not operator."
                    })
                } ?: this.safeCast<GroupMessageEvent>()?.sender?.let { it.isOperator() || it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<GroupMemberEvent>().member.let {
                        "member:${it.id} is not operator."
                    })
                } ?: this.safeCast<UserEvent>()?.user?.let { it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<UserEvent>().user.let {
                        "user:${it.id} is not super_user."
                    })
                } ?: false
                b
            }
            OWNER -> {
                val b = this.safeCast<GroupMemberEvent>()?.member?.let { it.isOwner() || it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<GroupMemberEvent>().member.let {
                        "member:${it.id} is not owner."
                    })
                } ?: this.safeCast<GroupMessageEvent>()?.sender?.let { it.isOperator() || it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<GroupMemberEvent>().member.let {
                        "member:${it.id} is not operator."
                    })
                } ?: this.safeCast<UserEvent>()?.user?.let { it.isSuperUser() }.also {
                    if (it == false) logger.debug(this.cast<UserEvent>().user.let {
                        "user:${it.id} is not super_user."
                    })
                } ?: false
                b
            }
            SUPERUSER -> {
                this.safeCast<UserEvent>()?.user?.isSuperUser()?:
                this.safeCast<GroupMemberEvent>()?.user?.isSuperUser()?:
                this.safeCast<MessageEvent>()?.sender.isSuperUser()
            }
            else -> {
                throw IllegalStateException(permission.name)
            }
        }
    }

}

@Suppress("unused")
open class Command(
    override val commandId: String,
    priority: Int = 5,
    classify: String = "",
    description: String = "",
    usage: String = "",
    superUserUsage: String = "",
    example: String = "",
) : AbstractCommand(commandId, priority, classify, usage, superUserUsage, example) {


    @Trigger0
    override suspend fun triggered(event: Event): Boolean {
        if (event !is MessageEvent) return event.getTriggeredFunctions("").isNotEmpty()
        return event.getTriggeredFunctions(event.message.content).isNotEmpty()
    }

    private suspend fun Event.getTriggeredFunctions(message: String): List<Triple<EventMatchResult, KFunction<*>, Command>> {
//        logger.debug("message = [${message}]")
        val triggers = this@Command::class.declaredMemberExtensionFunctions.filter { it.hasAnnotation<Trigger>() }
        val extensionFunctions =
            this@Command::class.declaredMemberExtensionFunctions.filter { it.hasAnnotation<Command>() }
        return extensionFunctions.mapNotNull { kFunction ->
            val annotation = kFunction.findAnnotation<Command>()!!

//            println("kFunction = ${kFunction.name}")
            var matches: EventMatchResult? = null
            val trigger = (triggers.firstOrNull { it.findAnnotation<Trigger>()?.targetFunctionName == kFunction.name }
                ?: triggers.singleOrNull())?.run {
                val kParameter = extensionReceiverParameter ?: parameters[1]
                if ((kParameter.type.classifier as KClass<*>).isSuperclassOf(this@getTriggeredFunctions::class)) this
                else {
//                        logger.debug((kParameter.type.classifier as KClass<*>).qualifiedName)
                    null
                }
            }
//            println("${commandId} triggers = ${triggers.map { it.name }}")
//            println("${commandId} trigger = ${trigger?.name}")
            if (trigger != null) trigger.run {
                if (isSuspend) {
                    callSuspend(this@Command, this@getTriggeredFunctions) as? EventMatchResult
                } else {
                    call(this@Command, this@getTriggeredFunctions) as? EventMatchResult
                }
            }?.let { matches = it } ?: return@mapNotNull null




            matches = matches ?: (annotation.regex.let { listOf(it) }.map { s ->
                Regex(s, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
            }.firstOrNull { regex -> regex.find(message) != null }?.find(message))?.let { EventMatchResult(it) }
            val b1 = matches != null

            val receiverParameter = kFunction.extensionReceiverParameter
            val classifier = receiverParameter?.type?.classifier
            val b2 = classifier != null && this::class.isSubclassOf(classifier as KClass<*>)
//            logger.debug("regexs = ${regexs}")
//            logger.debug("first match = ${matches?.groupValues}")
//            logger.debug("this::class.isSuperclassOf(classifier as KClass<*>) = ${b2}")
//            if (!b2) {
//                logger.warning("this::class = ${this::class}")
//                logger.warning("receiverParameter.type.classifier = ${receiverParameter?.type?.classifier as KClass<*>}")
//            }
            val b = b1 && b2
            if (b) Triple(matches!!, kFunction, annotation)
            else null
        }
    }

    override suspend fun checkPermission(event: Event): Boolean {
        if (event is UserEvent && event.user.isSuperUser()) return true

        val message = if (event is MessageEvent) event.message.content else ""
        val single = event.getTriggeredFunctions(message).ifEmpty { null }?.single() ?: return false

        if (event is StrangerEvent && config["allow_stranger", MainConfig.allowStranger]) {
            logger.warning("忽略陌生人消息: $event")
            return false
        }
        config["allow_friend", MainConfig.allowFriend]
        for (permission in single.second.findAnnotation<Command>()!!.permissions) {
            if (permission.ordinal <= 4) { // this should be single
                if (!event.checkTarget(permission)) {
                    //比如不允许私聊之类的
                    logger.warning("checkTarget($permission) failed")
                    return false
                }
            } else if (!event.checkTargetPermission(permission)) {
                logger.warning("checkTargetPermission($permission) failed")
                return false
            }
        }
        logger.debug("checkPermission passed")
        return true
    }

    @Runner
    suspend fun runC(event: Event): Pair<ExecutionResult, Int> {
        var coin: Int
        val message = if (event is MessageEvent) event.message.content else ""
        for ((matchResult, triggeredFunction, _annotation) in event.getTriggeredFunctions(message)) {
            coin = _annotation.coin
            if (event is MessageEvent) if (coin > Coins[event.sender]) {
                event.sendMessage("他吗 你原石不够了(%d)！！至少要%d个！".format(Coins[event.sender], coin))
                return ExecutionResult.LimitCall to coin
            }
            val any = try {
                when (triggeredFunction.parameters.size) {
                    3 -> {
                        val kParameter = triggeredFunction.parameters[2]
                        if ((kParameter.type.classifier as KClass<*>) == EventMatchResult::class) if (triggeredFunction.isSuspend) {
                            triggeredFunction.callSuspend(this, event, matchResult)
                        } else {
                            triggeredFunction.call(this, event, matchResult)
                        }
                        else throw IllegalArgumentException()
                    }
                    2 -> {
                        if (triggeredFunction.isSuspend) {
                            triggeredFunction.callSuspend(this, event)
                        } else {
                            triggeredFunction.call(this, event)
                        }
                    }
                    1 -> {
                        if (triggeredFunction.isSuspend) {
                            triggeredFunction.callSuspend(this)
                        } else {
                            triggeredFunction.call(this)
                        }
                    }
                    else -> throw IllegalArgumentException()
                }
            } catch (e: InvocationTargetException) {
                ExecutionResult.Failed(e.cause)
            } catch (e: Exception) {
                ExecutionResult.Error(e)
            }
            return (any?.safeCast<ExecutionResult>() ?: ExecutionResult.Unknown) to coin
        }
        return ExecutionResult.Error(IllegalStateException("no matching function")) to -1
    }


}


object TestCommand : Command(commandId = "name",
    priority = 5,
    classify = "class1",
    description = "none",
    usage = "none",
    superUserUsage = "none",
    example = "ss,fff,hhhc,cv") {
    val listOf: List<String>
        get() = listOf("砂糖", "辣辣", "番茄", "派蒙")
    val timer = Timer()

    @Command("test")
    suspend fun GroupMessageEvent.run2(eventMatchResult: EventMatchResult) {

        logger.debug {
            "{test}".placeholder("test" to "aaa", "vv" to "aa")
        }
        timer.schedule(timerTask {
            try {
                runBlocking {
                    MemberLeaveEvent.Quit(sender as NormalMember).broadcast()
                }
            } catch (_: Exception) {
            }
        }, 1000)

    }

    @Command("^(.+)酱$")
    suspend fun GroupMessageEvent.run(eventMatchResult: EventMatchResult) {

        val s = eventMatchResult[1]
        if (s in listOf) {
            val message1 = StringSubstitutor.replace("\${name}酱可爱捏~", mapOf("name" to s))
            sendMessage(message1)
        }
    }
}