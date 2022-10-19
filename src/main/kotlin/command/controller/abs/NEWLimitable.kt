package org.celery.command.controller.abs

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.command.controller.CommandBlockMode
import org.celery.command.controller.CommandBlockMode.*
import org.celery.config.main.function.*
import kotlin.contracts.ExperimentalContracts

interface NEWLimitable {
    val commandId: String
    private val logger: MiraiLogger
        get() = Rika.logger

    enum class LimitMode {
        //0 = global, 1 = subject, 2 = user in subject, 3 = user
        GLOBAL, SUBJECT, USER_IN_SUBJECT, USER

    }
    companion object {
        init {
            BanList.data.replaceAll { t, u ->
                if (t.matches(Regex("""^\w+\.banUser\.\d+\.temp$""")))
                    null
                else
                    u
            }
            BanList.save()
        }
        @Suppress("unused")
        private object Key {
            fun banId(subjectId: Long, userId: Long, newLimitable: NEWLimitable) = when {
                subjectId == 0L && userId != 0L -> {
                    "${newLimitable.commandId}.banUser.$userId"
                }
                subjectId != 0L && userId == 0L -> {
                    "${newLimitable.commandId}.banSubject.$subjectId"

                }
                subjectId != 0L && userId != 0L -> {
                    "${newLimitable.commandId}.ban.$subjectId.$userId"
                }
                else -> {
                    throw Exception()
                }
            }
            fun tempBanId(subjectId: Long, userId: Long, newLimitable: NEWLimitable) = when {
                subjectId == 0L && userId != 0L -> {
                    "${newLimitable.commandId}.banUser.$userId.temp"
                }
                subjectId != 0L && userId == 0L -> {
                    "${newLimitable.commandId}.banSubject.$subjectId.temp"

                }
                subjectId != 0L && userId != 0L -> {
                    "${newLimitable.commandId}.ban.$subjectId.$userId.temp"
                }
                else -> {
                    throw Exception()
                }
            }

            fun banAll(subjectId: Long, userId: Long): String = when {
                subjectId == 0L && userId != 0L -> {
                    "banall.banUser.$userId"
                }
                subjectId != 0L && userId == 0L -> {
                    "banall.banSubject.$subjectId"

                }
                subjectId != 0L && userId != 0L -> {
                    "banall.$subjectId.$userId"
                }
                else -> {
                    throw Exception()
                }
            }
        }
        fun banAll(subjectId: Long,userId: Long){
            BanList[Key.banAll(subjectId,userId)]=true
        }
        fun unbanAll(subjectId: Long,userId: Long){
            BanList[Key.banAll(subjectId,userId)]=false
        }
        val locker = mutableListOf<Pair<Pair<Long, Long>, String>>()
    }

    //enable globally
    fun enable(): Boolean {
        logger.verbose("$commandId.enable")
        if (EnableMap.getOrDefault(commandId, EnableMap["default", true])) return false
        EnableMap[commandId] = true
        return true
    }

    //close globally
    fun close(): Boolean {
        logger.verbose("$commandId.close")
        if (!EnableMap.getOrDefault(commandId, true)) return false
        EnableMap[commandId] = false
        return true
    }

    //check if commmand is closed globally
    fun isEnable(): Boolean {
        logger.verbose("$commandId.isEnable")
        EnableMap[commandId, EnableMap["default", true]]
        return EnableMap[commandId]
    }

    /**
     * close temporarily
     *
     * for customization in group
     */
    fun closeTemporarily(subjectId: Long): Boolean {
        logger.verbose("$commandId.closeTemporary()" + ", args: subjectId = [${subjectId}]")
        if (isEnable().not()) {
            logger.warning("${subjectId} can not close command $commandId because it has been closed globally.")
            return false
        }
        if (EnableMap.getOrDefault("$commandId.dis.$subjectId.*", false)) return false//already close
        EnableMap["$commandId.dis.$subjectId.*"] = true
        return true
    }

    /**
     * enable if closed temporarily
     *
     * for customization in group
     */
    fun cleanTemporary(subjectId: Long): Boolean {
        logger.verbose("$commandId.cleanTemporary()" + ", args: subjectId = [${subjectId}]")
        if (isEnable().not()) {
            logger.warning("${subjectId} can not enable command $commandId because it has been closed globally.")
            return false
        }
        if (EnableMap.getOrDefault("$commandId.dis.$subjectId.*", false)) return false//already enable
        EnableMap.data.remove("$commandId.dis.$subjectId.*")
        EnableMap.save()
        return true
    }

    fun checkBan(subjectId: Long, userId: Long): Boolean {
        logger.verbose("$commandId.checkBan()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        return isBanned(0, userId) || isBanned(subjectId, 0) || isBanned(subjectId, userId)
    }

    /**
     * check banned for Subject , User or User in Subject
     *
     * if subjectId == 0 check if user is banned globally
     *
     * if userId == 0 check if subject is banned globally
     */
    fun isBanned(subjectId: Long, userId: Long): Boolean {
        logger.verbose("$commandId.isBanned()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        if (BanList.getOrDefault(Key.banId(subjectId,userId,this), false)) {
            Rika.logger.verbose(Key.banId(subjectId,userId,this))
            logger.warning(Key.banId(subjectId,userId,this))
            return true
        }
        if (BanList.getOrNull<Boolean>(Key.banAll(subjectId,userId)) == true){
            Rika.logger.verbose(Key.banAll(subjectId,userId))
            logger.warning(Key.banAll(subjectId,userId))
            return true
        }
//        val b = when {
//            subjectId == 0L && userId != 0L -> {
//                val b = BanList.getOrDefault("$commandId.banUser.$userId", false)
//                if (b) Rika.logger.debug("$commandId.banUser.$userId")
//                b
//            }
//            subjectId != 0L && userId == 0L -> {
//                val b = BanList.getOrDefault("$commandId.banSubject.$subjectId", false)
//                if (b) Rika.logger.debug("$commandId.banSubject.$userId")
//                b
//
//            }
//            subjectId != 0L && userId != 0L -> {
//                val b = BanList.getOrDefault("$commandId.ban.$subjectId.$userId", false)
//                if (b) Rika.logger.debug("$commandId.ban.$subjectId.$userId")
//                b
//            }
//            else -> {
//                throw IllegalStateException()
//            }
//        }
        return false
    }

    /**
     * ban Subject , User or User in Subject
     *
     * if subjectId == 0 ban user globally
     *
     * if userId == 0 ban subject globally
     */
    fun ban(subjectId: Long, userId: Long) {
        logger.verbose("$commandId.ban()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        BanList[Key.banId(subjectId, userId, this)] = true
//        when {
//            subjectId == 0L && userId != 0L -> {
//                BanList["$commandId.banUser.$userId"] = true
//            }
//            subjectId != 0L && userId == 0L -> {
//                BanList["$commandId.banSubject.$subjectId"] = true
//
//            }
//            subjectId != 0L && userId != 0L -> {
//                BanList["$commandId.ban.$subjectId.$userId"] = true
//            }
//            else -> {
//                throw Exception()
//            }
//        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun temporaryBan(subjectId: Long, userId: Long, millionSeconds: Long) {
        BanList.set(Key.tempBanId(subjectId,userId,this), true)
        GlobalScope.launch {
            delay(millionSeconds)
            BanList.set(Key.tempBanId(subjectId,userId,this@NEWLimitable), false)
//            BanList.data.remove("temp.ban.$userId")
//            BanList.isModified.set(true)
        }
    }

    /**
     * unban Subject , User or User in Subject
     *
     * if subjectId == 0 ban user globally
     *
     * if userId == 0 ban subject globally
     */
    fun unban(subjectId: Long, userId: Long) {
        logger.verbose("$commandId.unban()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        BanList.data.remove(Key.banId(subjectId,userId,this))
//        when {
//            subjectId == 0L && userId != 0L -> {
//                BanList.data.remove("$commandId.banUser.$userId")
//            }
//            subjectId != 0L && userId == 0L -> {
//                BanList.data.remove("$commandId.banSubject.$subjectId")
//
//            }
//            subjectId != 0L && userId != 0L -> {
//                BanList.data.remove("$commandId.ban.$subjectId.$userId")
//            }
//            else -> {
//                throw Exception()
//            }
//        }
        BanList.isModified.set(true)
    }

    /**
     * get count limit mode of Subject , User or User in Subject
     * @return 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun getLimitMode(globalMode: Boolean): LimitMode? {
        logger.verbose("$commandId.getLimitMode()" + ", args: globalMode = [${globalMode}]")
        return if (globalMode) {
            CountLimit.getOrNull<LimitMode>("$commandId.mode.global")
//            when {
//                subjectId == 0L && userId == 0L -> {
//                }
//                else -> {
//                    throw Exception()
//                }
//            }
        } else {
            CountLimit.getOrDefault("$commandId.mode.normal", getDefaultLimitMode())
//            when {
//                subjectId != 0L && userId != 0L -> {
//                }
//                else -> {
//                    throw Exception()
//                }
//            }
        }
    }

    /**
     * set count limit mode of Subject , User or User in Subject
     * @return 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun setLimitMode(subjectId: Long, userId: Long, newMode: Int, globalMode: Boolean) {
        logger.verbose("$commandId.setLimitMode()" + ", args: subjectId = [${subjectId}], userId = [${userId}], newMode = [${newMode}], globalMode = [${globalMode}]")
        if (globalMode) {
            when {
                subjectId != 0L && userId != 0L -> {
                    CountLimit["$commandId.mode.global"] = newMode
                }
                else -> {
                    throw Exception()
                }
            }
        } else {
            when {
                subjectId != 0L && userId != 0L -> {
                    CountLimit["$commandId.mode.normal"] = newMode
                }
                else -> {
                    throw Exception()
                }
            }
        }
    }

    /**
     * get count limit mode of Subject , User or User in Subject
     * @return 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun getLimitMode(subjectId: Long, userId: Long): LimitMode {
        logger.verbose("$commandId.getLimitMode()")
        return CountLimit.getOrNull<LimitMode>("$commandId.limit_mode.except.u$userId")
            ?: CountLimit.getOrNull<LimitMode>("$commandId.limit_mode.except.s$subjectId")
            ?: CountLimit.getOrNull<LimitMode>("$commandId.limit_mode.except.s${subjectId}u$userId")
            ?: CountLimit.getOrDefault("$commandId.limit_mode.default", getDefaultLimitMode())
//        return getLimitMode(true) ?: getLimitMode(false)!!
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun getLimit(subjectId: Long, userId: Long, limitMode: LimitMode = getLimitMode(subjectId, userId)): Int {
        logger.verbose("$commandId.getLimit()" + ", args: subjectId = [${subjectId}], userId = [${userId}], limitMode = [${limitMode}]")
        val defaultValue = getDefaultLimit()
        return when (limitMode) {
            LimitMode.GLOBAL -> {
                CountLimit.getOrNull("$commandId.MAX_LIMIT.global", )?:defaultValue
            }
            LimitMode.SUBJECT -> {
                CountLimit.getOrNull("$commandId.MAX_LIMIT.$subjectId.*", )?:defaultValue
            }
            LimitMode.USER_IN_SUBJECT -> {
                CountLimit.getOrNull("$commandId.MAX_LIMIT.$subjectId.$userId", )?:defaultValue
            }
            LimitMode.USER -> {
                CountLimit.getOrNull("$commandId.MAX_LIMIT.*.$userId", )?:defaultValue
            }
        }
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun setLimit(
        subjectId: Long, userId: Long, value: Int, limitMode: LimitMode = getLimitMode(subjectId, userId)
    ): Boolean {
        logger.verbose("$commandId.setLimit()" + ", args: subjectId = [${subjectId}], userId = [${userId}], value = [$value], limitMode = [${limitMode}]")
        if (value == getDefaultLimit()) return false
        when (limitMode) {
            LimitMode.GLOBAL -> {
                CountLimit.set("$commandId.MAX_LIMIT.global", value)
            }
            LimitMode.SUBJECT -> {
                CountLimit.set("$commandId.MAX_LIMIT.$subjectId.*", value)
            }
            LimitMode.USER_IN_SUBJECT -> {
                CountLimit.set("$commandId.MAX_LIMIT.$subjectId.$userId", value)
            }
            LimitMode.USER -> {
                CountLimit.set("$commandId.MAX_LIMIT.*.$userId", value)
            }
            else -> {
                throw IllegalStateException("invalid limitmode: $limitMode")
            }
        }
        return true
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun increaseCount(subjectId: Long, userId: Long, limitMode: LimitMode) {
        logger.verbose("$commandId.increaseCount()" + ", args: subjectId = [${subjectId}], userId = [${userId}], limitMode = [${limitMode}]")
        val i = getCount(subjectId, userId, limitMode)
        when (limitMode.ordinal) {
            0 -> {
                CountRecords["$commandId.*.*"] = i + 1
                CountRecords["$commandId.*.*.last_time"] = System.currentTimeMillis()
            }
            1 -> {
                CountRecords["$commandId.$subjectId.*"] = i + 1
                CountRecords["$commandId.$subjectId.*.last_time"] = System.currentTimeMillis()
            }
            2 -> {
                CountRecords["$commandId.$subjectId.$userId"] = i + 1
                CountRecords["$commandId.$subjectId.$userId.last_time"] = System.currentTimeMillis()
            }
            3 -> {
                CountRecords["$commandId.*.$userId"] = i + 1
                CountRecords["$commandId.*.$userId.last_time"] = System.currentTimeMillis()
            }
            else -> {
                throw IllegalStateException("invalid limitmode: $limitMode")
            }
        }
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun decreaseCount(subjectId: Long, userId: Long, limitMode: LimitMode) {
        logger.verbose("$commandId.decreaseCount()" + ", args: subjectId = [${subjectId}], userId = [${userId}], limitMode = [${limitMode}]")
        val i = getCount(subjectId, userId, limitMode)
        if (i == 0) return
        when (limitMode.ordinal) {
            0 -> {
                CountRecords["$commandId.*.*"] = i - 1
                CountRecords["$commandId.*.*.last_time"] = System.currentTimeMillis()
            }
            1 -> {
                CountRecords["$commandId.$subjectId.*"] = i - 1
                CountRecords["$commandId.$subjectId.*.last_time"] = System.currentTimeMillis()
            }
            2 -> {
                CountRecords["$commandId.$subjectId.$userId"] = i - 1
                CountRecords["$commandId.$subjectId.$userId.last_time"] = System.currentTimeMillis()
            }
            3 -> {
                CountRecords["$commandId.*.$userId"] = i - 1
                CountRecords["$commandId.*.$userId.last_time"] = System.currentTimeMillis()
            }
            else -> {
                throw IllegalStateException("invalid limitmode: $limitMode")
            }
        }
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun getLastTime(subjectId: Long, userId: Long, limitMode: LimitMode): Long {
        logger.verbose("$commandId.getLastTime()" + ", args: subjectId = [${subjectId}], userId = [${userId}], limitMode = [${limitMode}]")
        return when (limitMode.ordinal) {
            0 -> {
                CountRecords["$commandId.*.*.last_time", 0L]
            }
            1 -> {
                CountRecords["$commandId.$subjectId.*.last_time", 0L]
            }
            2 -> {
                CountRecords["$commandId.$subjectId.$userId.last_time", 0L]
            }
            3 -> {
                CountRecords["$commandId.*.$userId.last_time", 0L]
            }
            else -> {
                throw IllegalStateException("invalid limitmode: $limitMode")
            }
        }
    }

    /**
     * @param limitMode 0 = global, 1 = subject, 2 = user in subject, 3 = user
     */
    fun getCount(subjectId: Long, userId: Long, limitMode: LimitMode): Int {
        logger.verbose("$commandId.getCount()" + ", args: subjectId = [${subjectId}], userId = [${userId}], limitMode = [${limitMode}]")
        return when (limitMode.ordinal) {
            0 -> {
                CountRecords["$commandId.*.*", 0]
            }
            1 -> {
                CountRecords["$commandId.$subjectId.*", 0]
            }
            2 -> {
                CountRecords["$commandId.$subjectId.$userId", 0]
            }
            3 -> {
                CountRecords["$commandId.*.$userId", 0]
            }
            else -> {
                throw IllegalStateException("invalid limitmode: $limitMode")
            }
        }
    }

    fun canCall(subjectId: Long, userId: Long): Boolean {
        logger.verbose("$commandId.canCall()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        if (EnableMap.getOrDefault("$commandId.dis.$subjectId.*", false)) {
            Rika.logger.debug("temp disable for subject $subjectId")
            return false
        }
        if (checkBan(subjectId, userId)) {
            Rika.logger.debug("subject $subjectId or user $userId is banned")
            return false
        }

        when (getBlockMode(true)) {
            BLACKLIST -> {
                if (!checkBlackList(subjectId, userId, true)) {
                    return false
                }
            }
            WHITELIST -> {
                if (!checkWhiteList(subjectId, userId, true)) {
                    return false
                }
            }
            EMPTY -> {
                TODO("not done")
                checkBlackList(subjectId, userId, true)
            }
            null -> {
                //NOTHING
            }
        }

        if (!when (getBlockMode(false)!!) {
                BLACKLIST -> checkBlackList(subjectId, userId, true) && checkBlackList(subjectId, userId, false)
                WHITELIST -> checkWhiteList(subjectId, userId, true) || checkWhiteList(subjectId, userId, false)
                EMPTY -> TODO("not done")
            }
        ) return false
        val limitMode = getLimitMode(subjectId, userId)
        val limit = getLimit(subjectId, userId, limitMode)
        if (getCount(subjectId, userId, limitMode) >= limit) {
            Rika.logger.debug("count limit($limit)")
            return false
        }
        if (System.currentTimeMillis() - getLastTime(subjectId, userId, limitMode) < getCooldown(commandId)) {
            Rika.logger.debug("call too fast")
            return false
        }
        return true
    }

    fun checkBlackList(subjectId: Long, userId: Long, global: Boolean): Boolean {
        if (global) {//check user
            if (BlackList.containsUser(userId)) {
                logger.warning("user:$userId in false blackList.")
                return false
            }
            //check subject
            if (BlackList.containsSubject(subjectId)) {
                logger.warning("subject:$subjectId in false blackList.")
                return false
            }
        } else {
            //check user in subject
            if (BlackList.containsUserInSubject(subjectId, userId)) {
                logger.warning("user:$userId in blackList of subject:$subjectId.")
                return false
            }
        }
        return true
    }

    fun checkWhiteList(subjectId: Long, userId: Long, global: Boolean): Boolean {
        var result = false
        val reason = mutableListOf<String>()
        if (global) {//check user
            if (WhiteList.containsUser(userId)) {
                result = true
            } else {
                reason += ("user:$userId not in global whitelist.")
            }
            //check subject
            if (!result && WhiteList.containsSubject(subjectId)) {
                result = true
            } else {
                reason += ("subject:$subjectId not in global whitelist.")
            }
        } else {
            //check user in subject
            if (WhiteList.containsUserInSubject(subjectId, userId)) {
                result = true
            } else {
                reason += ("user:$userId not in whitelist of subject:$subjectId.")
            }
        }
        if (!result) reason.forEach {
            logger.warning(it)
        }
        return result
    }

    fun getBlockMode(global: Boolean = false): CommandBlockMode? {
        return if (global) CountLimit.getOrDefault<CommandBlockMode?>(
            "$commandId.blockMode.global",null
        )
        else CountLimit.get(
            "$commandId.blockMode", CountLimit["default.blockmode", BLACKLIST]
        )
    }

    fun getCooldown(name: String = commandId) =
        CountLimit.getOrNull<Int>("$name.cooldown")?:CountLimit["default.cooldown", 1000]

    fun increaseCount(subjectId: Long, userId: Long) {
        logger.verbose("$commandId.increaseCount()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        increaseCount(subjectId, userId, getLimitMode(subjectId, userId))
    }

    fun decreaseCount(subjectId: Long, userId: Long) {
        logger.verbose("$commandId.decreaseCount()" + ", args: subjectId = [${subjectId}], userId = [${userId}]")
        decreaseCount(subjectId, userId, getLimitMode(subjectId, userId))
    }

    fun getDefaultLimit(): Int {
        logger.verbose("$commandId.getDefaultLimit()")
        return CountLimit["$commandId.default_limit", CountLimit["default.default_limit",10]]
    }

    fun setDefaultLimit(value: Int) {
        logger.verbose("$commandId.setDefaultLimit($value)")
        CountLimit["$commandId.default_limit"] = value
    }

    fun getDefaultLimitMode(): LimitMode {
        logger.verbose("$commandId.getDefaultLimitMode")
        return CountLimit.getOrDefault("default_limit_mode", CountLimit["default.default_limit_mode",LimitMode.SUBJECT])
    }
}

/**
 * 对于指定[subjectId]和[userId]，若[block]未执行完毕,下一次调用时将跳过[block]函数并将[subjectId]和[userId]封装为锁定[LockResult.Locked]返回
 *
 * 否则如果调用成功，则调用指定的函数block并返回其封装结果，捕获从block函数执行中抛出的任何Throwable异常并将其封装为失败
 */
@OptIn(ExperimentalContracts::class)
inline fun <R> NEWLimitable.withlock(
    subjectId: Long, userId: Long, commandId: String = this.commandId, block: () -> R
): LockResult<R> {
    val locked = synchronized(NEWLimitable.locker) {
        NEWLimitable.locker.contains(subjectId to userId to commandId).not()
    }
    return if (locked) {
        try {
            NEWLimitable.locker.add(subjectId to userId to commandId)
            LockResult.success(block())
        } catch (e: Exception) {
            LockResult.failure(e)
        } finally {
            NEWLimitable.locker.remove(subjectId to userId to commandId)
        }
    } else {
        Rika.logger.warning("$commandId is using")
        LockResult.locked(subjectId to userId to commandId)
    }
}
