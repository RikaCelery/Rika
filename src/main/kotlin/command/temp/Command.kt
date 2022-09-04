package org.celery.command.temp//package com.example.command
//
//import org.celery.rika.Rika
//import com.example.command.Command.CommandBlockMode.*
//import com.example.command.Command.Permission.*
//import com.celery.rika.config.main.MainConfig
//import com.celery.rika.utils.permission.isSuperUser
//import com.celery.rika.utils.time.TimeUtils
//import kotlinx.coroutines.runBlocking
//import net.mamoe.mirai.console.command.*
//import net.mamoe.mirai.console.command.Command
//import net.mamoe.mirai.contact.*
//import net.mamoe.mirai.event.events.GroupMessageEvent
//import net.mamoe.mirai.utils.MiraiLogger
//import net.mamoe.mirai.utils.debug
//import java.io.File
//import java.util.*
//import kotlin.reflect.jvm.jvmName
//
//interface Command : CommandInfo, Command {
//    companion object {
//
////        var BlockMode: CommandBlockMode = CommandBlockMode.BLACKLIST
////        var BlockMode: CommandBlockMode = CommandBlockMode.BLACKLIST
//
//        // 用于限制调用频率
//        private val logFile = File("log/call.log")
//        private val logger = MiraiLogger.Factory.create(this::class, "commandLogger")
//        var callHistory: Hashtable<CommandPrimaryName, MutableList<Call>> = Hashtable()
//
//        // 用于阻止重复调用
//        val blockUser = Hashtable<CommandPrimaryName, MutableList<User?>>()
//        val blockSubject = Hashtable<CommandPrimaryName, MutableList<Contact?>>()
//        val blockGlobal = Hashtable<CommandPrimaryName, Boolean>()
//    }
//
//    @kotlinx.serialization.Serializable
//    data class Call(val user: UserId?, val subject: GroupId?, val time: Long = System.currentTimeMillis().div(1000)) {
//        @Synchronized
//        fun log() {
//            logFile.appendText("user:${user},subject:${subject},time:${TimeUtils.now("yyyy-MM-dd HH:mm:ss")}\n")
//        }
//    }
//
//    var defultCountC: Hashtable<String, Int>
//    var callCd: Hashtable<String, Long>
//    var callCountLimit: Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>
//    var blackListUser: Hashtable<GroupId, MutableList<UserId>>
//    var blackListGroup: MutableList<GroupId>
//    var whiteListUser: Hashtable<GroupId, MutableList<UserId>>
//    var whiteListGroup: MutableList<GroupId>
//    var enable: Boolean
//    var blockMode: Hashtable<GroupId, CommandBlockMode>
//
//    suspend fun CommandSender.execute(block: suspend CommandSender.() -> Any)
//
//    /**
//     * 仅做调用记录和异常处理
//     */
//    suspend fun CommandSender.commonRun(block: suspend CommandSender.() -> Unit) {
//
//        addCall(true, block.getSubCommandName())
//        block()
//        val commandId: CommandPrimaryName
//        commandId =
//            if (this@Command is CompositeCommand) this@Command.primaryName + '.' + block.getSubCommandName() else this@Command.primaryName
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        callHistory[commandId]!!.add(Call(user?.id, subject?.id))
//    }
//
//    /**
//     * 阻塞用户
//     * @param blockSub 仅阻止子指令的重复调用(仅CompositeCommand有效)
//     * @param showTip 当命令被阻止时发送提示信息
//     */
//    suspend fun CommandSender.blockRunUser(
//        permission: Permission = NORMAL_MEMBER,
//        showTip: Boolean = false,
//        blockSub: Boolean = false,
//        checkCall: Boolean = true,
//        block: suspend CommandSender.() -> Unit,
//    ) {
//        if (!checkPermission0(permission)) {
////            logger.info("permission check faild.")
//            return
//        }
//        if (checkCall && !checkUserCall(blockSub, block.getSubCommandName())) {
//            if (logger.isDebugEnabled) logger.info("call check faild.")
//            return
//        }
//        if (this@Command is SimpleCommand || this@Command is CompositeCommand && !blockSub) {
//            val commandId = this@Command.primaryName
//            if (blockUser[commandId]?.contains(user) == true) {
////                if (logger.isDebugEnabled) logger.debug("blocked.")
//                logger.info("blocked.")
//                if (showTip) {
//                    sendMessage("你已经在使用这条命令了！")
//                }
//                return
//            } else {
//                blockUser[commandId]?.add(user) ?: blockUser.put(commandId, mutableListOf(user))
//                try {
//                    if (logger.isDebugEnabled) logger.debug("run func.")
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (_: Exception) {
//                }
//                blockUser[commandId]!!.remove(user)
//            }
//        } else if (this@Command is CompositeCommand) {
//            if (subject == null) {
//                if (logger.isDebugEnabled) logger.debug("run func.")
//
//                addCall(blockSub, block.getSubCommandName())
//                block()
//                return
//            }
//            val subName = block.getSubCommandName()
//            if (blockUser[subName]?.contains(user) == true) {
//                if (showTip) {
//                    if (logger.isDebugEnabled) logger.debug("blocked.")
//                    sendMessage("你已经在使用/${this@Command.primaryName} ${subName}这条命令了！")
//                }
//                return
//            } else {
//                blockUser[subName]?.add(user) ?: blockUser.put(subName, mutableListOf(user))
//                try {
//                    if (logger.isDebugEnabled) logger.debug("run func.")
//
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (_: Exception) {
//                }
//                blockUser[subName]!!.remove(user)
//            }
//        } else {
//            logger.warning("else-1, this shouldn't happen")
//        }
//    }
//
//    /**
//     * 阻塞对象
//     * @param blockSub 仅阻止子指令的重复调用(仅CompositeCommand有效)
//     * @param showTip 当命令被阻止时发送提示信息
//     */
//    suspend fun CommandSender.blockRunSubject(
//        permission: Permission = NORMAL_MEMBER,
//        showTip: Boolean = false,
//        blockSub: Boolean = false,
//        checkCall: Boolean = true,
//        block: suspend CommandSender.() -> Unit,
//    ) {
//        if (!checkPermission0(permission)) {
//            if (logger.isDebugEnabled) logger.debug("permission check faild.")
//            return
//        }
//        if (checkCall && !checkGroupCall(blockSub, block.getSubCommandName())) {
//            if (logger.isDebugEnabled) logger.debug("call check faild.")
//            return
//        }
//        if (this@Command is SimpleCommand || this@Command is CompositeCommand && !blockSub) {
//            val commandId = this@Command.primaryName
//            if (blockSubject[commandId]?.contains(subject) == true) {
//                if (showTip) {
//                    if (logger.isDebugEnabled) logger.debug("blocked.")
//                    sendMessage("群内其他人正在使用这条命令！")
//                }
//                return
//            } else {
//                blockSubject[commandId]?.add(subject) ?: blockSubject.put(commandId, mutableListOf(subject))
//                try {
//                    if (logger.isDebugEnabled) logger.debug("run func.")
//
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    sendMessage("指令执行出错!${e::class.simpleName}")
//                }
//                blockSubject[commandId]!!.remove(subject)
//            }
//        } else if (this@Command is CompositeCommand) {
//            if (subject == null) {
//                if (logger.isDebugEnabled) logger.debug("run func.")
//
//                addCall(blockSub, block.getSubCommandName())
//                block()
//                return
//            }
//            val subName = block.getSubCommandName()
//            if (blockSubject[subName]?.contains(subject) == true) {
//                if (showTip) {
//                    if (logger.isDebugEnabled) logger.debug("blocked.")
//                    sendMessage("群内其他人正在使用/${this@Command.primaryName} ${subName}这条命令！")
//                }
//                return
//            } else {
//                blockSubject[subName]?.add(subject) ?: blockSubject.put(subName, mutableListOf(subject))
//                try {
//                    if (logger.isDebugEnabled) logger.debug("run func.")
//
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    sendMessage("指令执行出错!${e::class.simpleName}")
//                }
//                blockSubject[subName]!!.remove(subject)
//            }
//        } else {
//            logger.warning("else-1, this shouldn't happen")
//        }
//    }
//
//    /**
//     * 全局阻塞
//     * @param blockSub 仅阻止子指令的重复调用(仅CompositeCommand有效)
//     * @param showTip 当命令被阻止时发送提示信息
//     */
//    suspend fun CommandSender.blockRunGlobal(
//        vararg permission: Permission = arrayOf(NORMAL_MEMBER, FRIEND_ONLY),
//        showTip: Boolean = false,
//        blockSub: Boolean = false,
//        checkCall: Boolean = true,
//        block: suspend CommandSender.() -> Unit,
//    ) {
//        if (!checkRulesIfAnyPassed(permission)) {
//            if (logger.isDebugEnabled) logger.debug("permission check faild.")
//            return
//        }
//        if (checkCall && !checkGlobalCall(blockSub, block.getSubCommandName())) {
//            if (logger.isDebugEnabled) logger.debug("call check faild.")
//            return
//        }
//        if (this@Command is SimpleCommand || this@Command is CompositeCommand && !blockSub) {
//            if (logger.isDebugEnabled) logger.debug("if-1:${this@Command::class.simpleName}, blockSub:$blockSub")
//            val commandId = this@Command.primaryName
//            if (blockGlobal[commandId] == true) {
//                if (logger.isDebugEnabled) logger.debug("blocked")
//                if (showTip) {
//                    sendMessage("有其他人正在使用这条命令！")
//                }
//                return
//            } else {
//                blockGlobal[commandId] = true
//                try {
//                    if (logger.isDebugEnabled) if (logger.isDebugEnabled) logger.debug("run func")
//
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (_: Exception) {
//                }
//                blockGlobal[commandId] = false
//            }
//        } else if (this@Command is CompositeCommand) {
//            if (logger.isDebugEnabled) logger.debug("else if-1")
//            if (subject == null) {
//                if (logger.isDebugEnabled)
//                    addCall(blockSub, block.getSubCommandName())
//                block()
//                return
//            }
//            block.getSubCommandName()
//            val subName = block.getSubCommandName()
//            if (blockGlobal[subName] == true) {
//                if (logger.isDebugEnabled) logger.debug("blocked")
//                if (showTip) {
//                    sendMessage("有其他人正在使用/${this@Command.primaryName} ${subName}这条命令！")
//                }
//                return
//            } else {
//                blockGlobal[subName] = true
//                try {
//
//                    if (logger.isDebugEnabled) logger.debug("run func")
//
//                    addCall(blockSub, block.getSubCommandName())
//                    block()
//                } catch (_: Exception) {
//                }
//                blockGlobal[subName] = false
//            }
//
//        } else {
//            logger.warning("else-1, this shouldn't happen")
//        }
//    }
//
//    fun CommandSender.setLimit(commandId: String, subject: GroupId, user: UserId, count: Int) {
//        val getCommands = Rika.getCommands(commandId)!!.single()
//        if (getCommands.callCountLimit[commandId] == null) getCommands.callCountLimit[commandId] = Hashtable()
//        if (getCommands.callCountLimit[commandId]!![subject] == null) getCommands.callCountLimit[commandId]!![subject] =
//            mutableMapOf()
//        logger.debug("setLimit:$commandId-$subject-$user->$count")
//        getCommands.callCountLimit[commandId]!![subject]!![user] = count
//
//    }
//
//    /**
//     * 检查调用频率是否合理
//     *
//     * 检查调用次数是否超过最大限制
//     *
//     * 超级用户和控制台不做任何检查
//     */
//    private fun CommandSender.checkGlobalCall(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (checkGlobalCall0(blockSub, subCommandSubName)) {
//            return true
//        } else {
//            return false
//        }
//    }
//
//    private fun CommandSender.addCall(blockSub: Boolean, subCommandSubName: CommandSubName) {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//        val call = Call(user?.id, subject?.id)
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        callHistory[commandId]!!.add(call)
//        call.log()
//        logger.debug { "call added $call, sizeNow: ${callHistory[commandId]!!.size}" }
//    }
//
//    private fun CommandSender.checkGroupCall(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (checkGroupCall0(blockSub, subCommandSubName)) {
//            return true
//        } else {
//            return false
//        }
//    }
//
//    private fun CommandSender.checkUserCall(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (checkUserCall0(blockSub, subCommandSubName)) {
//            return true
//        } else {
//            return false
//        }
//    }
//
//    private fun CommandSender.checkGlobalCall0(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (this.user == null) return true
//        if (!checkGroupCallFrequency(blockSub, subCommandSubName)) return false
//        if (!checkGroupCallCount(blockSub, subCommandSubName)) return false
//        return true
//    }
//
//    private fun CommandSender.checkGroupCall0(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (this.user == null) return true
//        if (!checkGroupCallFrequency(blockSub, subCommandSubName)) return false
//        if (!checkGroupCallCount(blockSub, subCommandSubName)) return false
//        return true
//    }
//
//    private fun CommandSender.checkUserCall0(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        if (this.user == null) return true
//        if (!checkUserCallFrequency(blockSub, subCommandSubName)) return false
//        if (!checkUserCallCount(blockSub, subCommandSubName)) return false
//        return true
//    }
//
//    private fun CommandSender.checkGlobalCallFrequency(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//        this.user
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        val call1 = callHistory[commandId]!!.lastOrNull()
//        if (call1 == null) return true
//        val minCd = callCd[commandId] ?: MainConfig.defultCallCd
//        val timeNow = System.currentTimeMillis().div(1000)
//        logger.debug {
//            commandId + ", lastCallTime: ${call1.time}, thisCallTime: ${timeNow}, delta: ${
//                timeNow - call1.time
//            }, minCd: $minCd"
//        }
//        val b = timeNow - call1.time >= minCd
//        logger.debug { "checkGlobalCallFrequency: " + b }
//        return b
//    }
//
//    private fun CommandSender.checkGlobalCallCount(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//        val subjectId = 0L
//        val userId = 0L
//        if (callCountLimit[commandId] == null) callCountLimit[commandId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId] == null) callCountLimit[commandId]!![subjectId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId]!![userId] == null) callCountLimit[commandId]!![subjectId]!![userId] =
//            MainConfig.defultCallLimit
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        logger.debug { commandId + ", callHistorySize: ${callHistory[commandId]!!.size}, callLimit: ${callCountLimit[commandId]!![subjectId]!![userId]!!}" }
////        logger.debug {
////            commandId + ", call: " + (callHistory[commandId]?.ifEmpty { null }?.joinToString("\n", "\n") ?: "")
////        }
//        val b = callHistory[commandId]!!.size <= callCountLimit[commandId]!![subjectId]!![userId]!!
//        logger.debug { "checkGlobalCallCount: " + b }
//        if (callHistory[commandId]!!.size == callCountLimit[commandId]!![subjectId]!![userId]!!) {
//            try {
//                runBlocking {
//                    sendMessage("你已经达到了${commandId}指令限制次数(${callCountLimit[commandId]!![subjectId]!![userId]!!}),您的后续${commandId}将会被忽略")
//                }
//            } catch (_: Exception) {
//            }
//        }
//        return b
//    }
//
//    private fun CommandSender.checkGroupCallFrequency(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        val call1 = callHistory[commandId]!!.filter { it.subject == subject?.id }.lastOrNull()
//        if (call1 == null) return true
//        val minCd = callCd[commandId] ?: MainConfig.defultCallCd
//        val timeNow = System.currentTimeMillis().div(1000)
//        logger.debug {
//            commandId + ", lastCallTime: ${call1.time}, thisCallTime: ${timeNow}, delta: ${
//                timeNow - call1.time
//            }, minCd: $minCd"
//        }
//        val b = timeNow - call1.time >= minCd
//        logger.debug { "checkGroupCallFrequency: " + b }
//        return b
//    }
//
//    private fun CommandSender.checkGroupCallCount(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//        val subjectId = subject?.id?: 0L
//        val userId = user?.id?:0L
//        if (callCountLimit[commandId] == null) callCountLimit[commandId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId] == null) callCountLimit[commandId]!![subjectId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId]!![userId] == null) callCountLimit[commandId]!![subjectId]!![userId] =
//            MainConfig.defultCallLimit
//        val calls = callHistory[commandId]?.filter { it.subject == subject?.id }
//        if (calls == null) callHistory[commandId] = mutableListOf()
//        logger.debug { commandId + ", callHistorySize: ${calls!!.size}, callLimit: ${callCountLimit[commandId]!![subjectId]!![userId]!!}" }
////        logger.debug {
////            commandId + ", call: " + (calls?.ifEmpty { null }?.joinToString("\n", "\n") ?: "")
////        }
//        val b = calls!!.size <= callCountLimit[commandId]!![subjectId]!![userId]!!
//        logger.debug { "checkGroupCallCount: " + b }
//        if (calls.size == callCountLimit[commandId]!![subjectId]!![userId]!!) {
//            try {
//                runBlocking {
//                    sendMessage("你已经达到了${commandId}指令限制次数(${callCountLimit[commandId]!![subjectId]!![userId]!!}),您的后续${commandId}将会被忽略")
//                }
//            } catch (_: Exception) {
//            }
//        }
//        return b
//    }
//
//    private fun CommandSender.checkUserCallFrequency(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//
//        if (callHistory[commandId] == null) callHistory[commandId] = mutableListOf()
//        val call1 = callHistory[commandId]!!.filter { it.subject == subject?.id && it.user == user?.id }.lastOrNull()
//        if (call1 == null) return true
//        val minCd = callCd[commandId] ?: MainConfig.defultCallCd
//        val timeNow = System.currentTimeMillis().div(1000)
//        logger.debug {
//            commandId + ", lastCallTime: ${call1.time}, thisCallTime: ${timeNow}, delta: ${
//                timeNow - call1.time
//            }, minCd: $minCd"
//        }
//        val b = timeNow - call1.time >= minCd
//        logger.debug { "checkUserCallFrequency: " + b }
//        return b
//    }
//
//    private fun CommandSender.checkUserCallCount(blockSub: Boolean, subCommandSubName: CommandSubName): Boolean {
//        val commandId: CommandPrimaryName
//        commandId = if (blockSub) this@Command.primaryName + '.' + subCommandSubName else this@Command.primaryName
//        val subjectId = subject?.id ?: 0L
//        val userId = user?.id ?: 0L
//        if (callCountLimit[commandId] == null) callCountLimit[commandId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId] == null) callCountLimit[commandId]!![subjectId] = mutableMapOf()
//        if (callCountLimit[commandId]!![subjectId]!![userId] == null) callCountLimit[commandId]!![subjectId]!![userId] =
//            MainConfig.defultCallLimit
//        val calls = callHistory[commandId]?.filter { it.subject == subject?.id && it.user == user?.id }
//        if (calls == null) callHistory[commandId] = mutableListOf()
//        logger.debug { commandId + ", callHistorySize: ${calls!!.size}, callLimit: ${callCountLimit[commandId]!![subjectId]!![userId]!!}" }
////        logger.debug {
////            commandId + ", call: " + (calls?.ifEmpty { null }?.joinToString("\n", "\n") ?: "")
////        }
//        val b = calls!!.size <= callCountLimit[commandId]!![subjectId]!![userId]!!
//        logger.debug { "checkUserCallCount: " + b }
//        if (calls.size == callCountLimit[commandId]!![subjectId]!![userId]!!) {
//            try {
//                runBlocking {
//                    sendMessage("你已经达到了${commandId}指令限制次数(${callCountLimit[commandId]!![subjectId]!![userId]!!}),您的后续${commandId}将会被忽略")
//                }
//            } catch (_: Exception) {
//            }
//        }
//        return b
//    }
//
//    /**
//     * 检查使用者权限
//     *
//     * **指令关闭** 或 **在黑名单** 或 **不在白名单** 时也会返回 false
//     *
//     * Console 在任何时候都返回 true
//     * @return true-可以调用
//     */
//    private suspend fun checkPermission(reviver: CommandSender, permission: Permission): Boolean =
//        reviver.checkPermission0(permission)
//
//    /**
//     * 任何一条规则匹配都会返回true
//     */
//    private suspend fun CommandSender.checkRulesIfAnyPassed(permission: Array<out Permission>): Boolean {
//        return permission.any { checkPermission0(it) }
//    }
//
//    private suspend fun CommandSender.checkPermission0(permission: Permission): Boolean {
////        logger.debug { "start permission check for $subject $user in ${this@Command.primaryName} (${this@Command.enable})." }
//        // console
//        if (!enable) {
//            if (subject == null) {
//                sendMessage("命令已被关闭")
//            }
//            logger.info("permission check faild,命令已被关闭.")
//            return false
//        }
//        if (subject == null) return true
//
//        val groupId = subject!!.id
//        val userId = user!!.id
//        if (whiteListUser[0] == null)
//            if (blackListUser[0] == null) blackListUser[0] = mutableListOf()
//        if (whiteListUser[groupId] == null) whiteListUser[groupId] = mutableListOf()
//        if (blackListUser[groupId] == null) blackListUser[groupId] = mutableListOf()
//        // globalBlackList 检查全局规则
//        if (blockMode[0] == null) blockMode[0] = BLACKLIST
//        when (blockMode[0]) {
//            WHITELIST -> {
//                if (whiteListUser[0] != null && userId !in whiteListUser[0]!!) {
//                    logger.info("permission check faild, user not in whiteList(global).")
//                    return false
//                }
//                if (groupId !in whiteListGroup) {
//                    logger.info("permission check faild, group not in whiteList.")
//                    return false
//                }
//            }
//            BLACKLIST -> {
//                if (userId in blackListUser[0]!!) {
//                    logger.info("permission check faild, user in blackList(global).")
//                    return false
//                }
//                if (groupId in blackListGroup) {
//                    logger.info("permission check faild, group in blackList.")
//                    return false
//                }
//            }
//            EMPTY -> {
//                blockMode.remove(0)
//                blockMode[groupId] = BLACKLIST
//            }
//            else -> {
//                //this shouldn't happen
//                throw IllegalStateException()
//            }
//        }
//        // groupBlackList 检查群自定义规则
//        if (blockMode[groupId] == null) blockMode[groupId] = BLACKLIST
//        when (blockMode[groupId]) {
//            WHITELIST -> {
//                if (userId !in whiteListUser[groupId]!!) {
//                    logger.info("permission check faild, user not in whiteList.")
//                    return false
//                }
//                if (groupId !in whiteListGroup) {
//                    logger.info("permission check faild, groupId not in whiteList.")
//                    return false
//                }
//            }
//            BLACKLIST -> {
//                if (userId in blackListUser[groupId]!!) {
//                    logger.info("permission check faild, user in blackList.")
//                    return false
//                }
//                if (groupId in blackListGroup) {
//                    logger.info("permission check faild 401.")
//                    return false
//                }
//            }
//            else -> {
//                //this shouldn't happen
//                throw IllegalStateException()
//            }
//        }
//
//        // 检查调用者权限
//        when (val event = (this as CommandSenderOnMessage<*>).fromEvent) {
//            is GroupMessageEvent -> {
//                // 群聊
//                return when (permission) {
//                    SUPER_USER -> user.isSuperUser()
//                    OWNER -> event.sender.isOwner() || user.isSuperUser()
//                    OPERATOR -> event.sender.isOperator() || user.isSuperUser()
//                    NORMAL_MEMBER -> event.sender is NormalMember
//                    ANY_MEMBER -> true
//                    FRIEND_ONLY -> {
//                        logger.debug("permission check faild. friend_only")
//                        false
//                    }
//                }
//            }
//            else -> {
//                // 私聊
//                return when (permission) {
//                    SUPER_USER -> {
//                        fromEvent.sender.isSuperUser()
//                    }
//                    FRIEND_ONLY -> {
//                        // 黑白名单已经处理过了 直接放行
//                        true
//                    }
//                    else -> {
//                        logger.debug("permission check faild. 私聊 not down yet.")
//                        false
//                    }
//                }
//            }
//        }
//
//    }
//
//    /**
//     * 从CommandSenderOnMessage<*> 中获取调用时的子指令名
//     *
//     * (将消息以' '分隔，取第二个值)
//     */
//    private fun (suspend CommandSender.() -> Unit).getSubCommandName(): CommandSubName {
//        // get function name
//        val last = (this)::class.jvmName.split("$").dropLast(1).last()
//        logger.debug("subCommandName: " + last)
//        return last
//    }
//
//    enum class Permission {
//
//        /**
//         * 仅Bot的超级管理员可使用(超级管理员在任何时候都可使用任何命令)
//         */
//        SUPER_USER,
//
//        /**
//         * 仅群主可使用
//         */
//        OWNER,
//
//        /**
//         * 管理员和群主可使用
//         */
//        OPERATOR,
//
//        /**
//         * 普通群员可使用
//         */
//        NORMAL_MEMBER,
//
//        /**
//         * 群员(包括匿名)可使用
//         */
//        ANY_MEMBER,
//
//        /**
//         * 仅私聊可用
//         */
//        FRIEND_ONLY
//    }
//
//    enum class BlockRunMode {
//        Global, Subject, User
//    }
//
//    enum class CommandBlockMode {
//        BLACKLIST, WHITELIST, EMPTY,
//    }
//}
//
//
//private typealias CommandPrimaryName = String
//private typealias CommandSubName = String
//private typealias GroupId = Long
//private typealias UserId = Long
