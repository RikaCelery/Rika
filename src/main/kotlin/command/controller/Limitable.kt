package org.celery.command.controller

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import net.mamoe.mirai.console.util.ContactUtils.getContactOrNull
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.debug
import org.celery.Rika
import org.celery.command.controller.BlockRunMode.*
import org.celery.command.controller.CommandBlockMode.*
import java.io.File
import java.util.*

/**
 * 根据[commandId]控制调用频率和调用次数
 */
interface Limitable {
    companion object {
        //调用历史记录
        var callHistory = Hashtable<String, MutableList<Call>>()

        // 暂时关闭的群
        var tempDisable = Hashtable<String, MutableList<Long>>()
        private var enableMap = Hashtable<String,Boolean>()

        // 权限管理(黑白名单)
        var blockMode = Hashtable<String, MutableMap<Long, CommandBlockMode>>()
        var blackListSubject = Hashtable<String, MutableList<Long>>()
        var blackListSubjectToUser = Hashtable<String, MutableMap<Long, MutableList<Long>>>()
        var whiteListSubject = Hashtable<String, MutableList<Long>>()
        var whiteListUser = Hashtable<String, MutableMap<Long, MutableList<Long>>>()

        // 调用限制信息
        var callCountLimitMap = Hashtable<String, Int>()
        var callCountLimitSubjectMap = Hashtable<String, MutableMap<Long, Int>>()
        var callCountLimitSubjectToUserMap = Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>()

        // 调用限制
        var blockRunMode = Hashtable<String, BlockRunMode>()
        var blockUser = Hashtable<String, MutableList<Long?>>()
        var blockSubject = Hashtable<String, MutableList<Long?>>()
        var blockGlobal = Hashtable<String, Boolean>()



        // json序列化器
        private val jsonSerializer = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            serializersModule += SerializersModule {
                Call.serializer()
            }
        }
        private val callHistoryFile = Rika.configFolder.resolve("limitation/callHistory.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val tempDisableFile = Rika.configFolder.resolve("limitation/tempDisable.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val blockModeFile = Rika.configFolder.resolve("limitation/blockMode.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val blackListSubjectFile = Rika.configFolder.resolve("limitation/blackListSubject.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val blackListUserFile = Rika.configFolder.resolve("limitation/blackListUser.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val whiteListSubjectFile = Rika.configFolder.resolve("limitation/whiteListSubject.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val whiteListUserFile = Rika.configFolder.resolve("limitation/whiteListUser.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val callCountLimitMapFile = Rika.configFolder.resolve("limitation/callCountLimitMap.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val callCountLimitSubjectMapFile = Rika.configFolder.resolve("limitation/callCountLimitSubjectMap.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val callCountLimitSubjectToUserMapFile =
            Rika.configFolder.resolve("limitation/callCountLimitSubjectToUser.json")
                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val blockRunModeFile = Rika.configFolder.resolve("limitation/blockRunMode.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
        private val enableMapFile = Rika.configFolder.resolve("limitation/enableMap.json")
            .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }

        /**
         * 用于从文件中加载共用数据
         */
        private fun load() {
            callHistory = jsonSerializer.decodeFromString<Map<String, MutableList<Call>>>(callHistoryFile.readText0())
                .toHashTable()
            tempDisable =
                (jsonSerializer.decodeFromString<MutableMap<String, MutableList<Long>>>(tempDisableFile.readText0())).toHashTable()

            callCountLimitMap =
                jsonSerializer.decodeFromString<Map<String, Int>>(callCountLimitMapFile.readText0()).toHashTable()
            callCountLimitSubjectMap =
                jsonSerializer.decodeFromString<Map<String, Map<Long, Int>>>(callCountLimitSubjectMapFile.readText0())
                    .mapValues { it.value.toHashTable() }.toHashTable()
            callCountLimitSubjectToUserMap =
                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableMap<Long, Int>>>>(
                    callCountLimitSubjectToUserMapFile.readText0()
                ).toHashTable()

            blockMode =
                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, CommandBlockMode>>>(blockModeFile.readText0())
                    .toHashTable()
            blackListSubject =
                jsonSerializer.decodeFromString<Map<String, MutableList<Long>>>(blackListSubjectFile.readText0())
                    .toHashTable()
            blackListSubjectToUser =
                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableList<Long>>>>(blackListUserFile.readText0())
                    .toHashTable()
            whiteListSubject =
                jsonSerializer.decodeFromString<Map<String, MutableList<Long>>>(whiteListSubjectFile.readText0())
                    .toHashTable()
            whiteListUser =
                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableList<Long>>>>(whiteListUserFile.readText0())
                    .toHashTable()

            blockRunMode =
                jsonSerializer.decodeFromString<Map<String, BlockRunMode>>(blockRunModeFile.readText0()).toHashTable()
            enableMap = jsonSerializer.decodeFromString<Map<String, Boolean>>(enableMapFile.readText0()).toHashTable()
        }

        /**
         * 用于将共用数据保存至文件
         */
        fun save() {
            callHistoryFile.writeText(jsonSerializer.encodeToString(callHistory.toMap()))
            tempDisableFile.writeText(jsonSerializer.encodeToString(tempDisable.toMap()))

            callCountLimitMapFile.writeText(jsonSerializer.encodeToString(callCountLimitMap.toMap()))
            callCountLimitSubjectMapFile.writeText(jsonSerializer.encodeToString(callCountLimitSubjectMap.toMap()))
            callCountLimitSubjectToUserMapFile.writeText(jsonSerializer.encodeToString(callCountLimitSubjectToUserMap.toMap()))

            blockModeFile.writeText(jsonSerializer.encodeToString(blockMode.toMap()))
            blackListSubjectFile.writeText(jsonSerializer.encodeToString(blackListSubject.toMap()))
            blackListUserFile.writeText(jsonSerializer.encodeToString(blackListSubjectToUser.toMap()))
            whiteListSubjectFile.writeText(jsonSerializer.encodeToString(whiteListSubject.toMap()))
            whiteListUserFile.writeText(jsonSerializer.encodeToString(whiteListUser.toMap()))

            blockRunModeFile.writeText(jsonSerializer.encodeToString(blockRunMode.toMap()))
//            blockUserFile.writeText(jsonSerializer.encodeToString(blockUser.toMap()))
//            blockSubjectFile.writeText(jsonSerializer.encodeToString(blockSubject.toMap()))
//            blockGlobalFile.writeText(jsonSerializer.encodeToString(blockGlobal.toMap()))


            enableMapFile.writeText(jsonSerializer.encodeToString<Map<String, Boolean>>(enableMap))
        }

        fun reload() {
            try {
                load()
            } catch (e:Exception) {
                e.printStackTrace()
                Rika.configFolder.resolve("limitation").listFiles()?.forEach(File::deleteRecursively)
                save()
            }
        }

        /**
         * Map to HashTable
         */
        private fun <K, V> Map<K, V>.toHashTable(): Hashtable<K, V> {
            val hashtable = Hashtable<K, V>()
            forEach { (t, u) ->
                hashtable[t] = u
            }
            return hashtable
        }
    }

    fun disableFor(subject: Long): Boolean {
        if (tempDisable[commandId]?.contains(subject) == true)
            return false
        tempDisable[commandId]?.add(subject) ?: tempDisable.put(commandId, mutableListOf(subject))
        return true
    }

    fun enableFor(subject: Long): Boolean {
        return (tempDisable[commandId]?.remove(subject) ?: false)
    }

    fun getEnable(): Boolean {
        return defaultEnable
    }

    fun disable() {
        Rika.logger.debug("$commandId disabled.")
        defaultEnable = false
    }
    fun enable() {
        Rika.logger.debug("$commandId enabled.")
        defaultEnable = true
    }

    /**
     * 用于唯一确定一个指令的名字
     */
    val commandId: String

    /**
     * 控制命令的启用或关闭（全局）
     */
    var defaultEnable: Boolean

    /**
     * 默认的最多调用次数,在[callCountLimitMap]中不存在对应值时取这个
     */
    var defaultCountLimit: Int

    /**
     * 阻塞调用方式,固定不可变
     */
    var defaultCallCountLimitMode: BlockRunMode

    /**
     * 阻塞调用方式,固定不可变
     */
    var defaultBlockRunModeMode: BlockRunMode
    /**
     * 默认的冷却时长,在[]中不存在对应值时取这个 毫秒
     */
    var defaultCoolDown: Long

    var showTip: Boolean


    fun getBlockRunMode(commandId: String) = blockRunMode[commandId] ?: this.defaultBlockRunModeMode
    fun getCoolDown(commandId: String) = defaultCoolDown
    fun getCallSize(call: Call) = getCallSizeOrNull(call)?:0
    fun getCountLimit(commandId: String) = callCountLimitMap[commandId]
    fun getCountLimit(call: Call): Int = getCountLimit(call.commandId, call.subjectId, call.userId)
    fun getCountLimit(id: String, groupId: Long?, userId: Long?): Int {

        val userLimit = callCountLimitSubjectToUserMap[id]?.get(groupId)?.get(userId)
        if (userLimit != null)
            return userLimit
        val groupLimit = callCountLimitSubjectMap[id]?.get(groupId)
        if (groupLimit != null)
            return groupLimit
        val globalLimit = callCountLimitMap[id]
        if (globalLimit != null)
            return globalLimit
        return defaultCountLimit
    }

    fun getCallSizeOrNull(call: Call,mode: BlockRunMode): Int? {
        val size = when (mode) {
            Global -> callHistory[call.commandId]?.size
            Subject -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId }?.size
            User -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId && it.userId == call.userId }?.size
        }
        return size
    }

    fun setUserCountLimit(subject: Long, userId: Long, limit: Int) {
        if (callCountLimitSubjectToUserMap[commandId] == null)
            callCountLimitSubjectToUserMap[commandId] = mutableMapOf()
        if (callCountLimitSubjectToUserMap[commandId]!![subject] == null)
            callCountLimitSubjectToUserMap[commandId]!![subject] = mutableMapOf()
        callCountLimitSubjectToUserMap[commandId]!![subject]!![userId] = limit
    }

    fun getUserCountLimit(subject: Long, userId: Long): Int? {
        if (callCountLimitSubjectToUserMap[commandId] == null)
            callCountLimitSubjectToUserMap[commandId] = mutableMapOf()
        if (callCountLimitSubjectToUserMap[commandId]!![subject] == null)
            callCountLimitSubjectToUserMap[commandId]!![subject] = mutableMapOf()
        return callCountLimitSubjectToUserMap[commandId]!![subject]!![userId]
    }

    fun setGroupCountLimit(subject: Long, limit: Int) {
        if (callCountLimitSubjectMap[commandId] == null)
            callCountLimitSubjectMap[commandId] = mutableMapOf()
        callCountLimitSubjectMap[commandId]!![subject] = limit
    }

    fun getGroupCountLimit(subject: Long): Int? {
        if (callCountLimitSubjectMap[commandId] == null)
            callCountLimitSubjectMap[commandId] = mutableMapOf()
        return callCountLimitSubjectMap[commandId]!![subject]
    }

    fun setGlobalLimit(limit: Int) {
        callCountLimitMap[commandId] = limit
    }

    fun getGlobalLimit(): Int? {
        return callCountLimitMap[commandId]
    }

    fun addBlackUserGlobal(userId: Long): Boolean {
        return addBlackUserInGroup(0, userId)
    }

    fun removeBlackUserGlobal(userId: Long): Boolean {
        return removeBlackUserInGroup(0, userId)
    }

    fun addBlackUserInGroup(groupId: Long, userId: Long): Boolean {
        if (blackListSubjectToUser[commandId] == null) {
            blackListSubjectToUser[commandId] = mutableMapOf()
        }
        if (blackListSubjectToUser[commandId]!![groupId] == null) {
            blackListSubjectToUser[commandId]!![groupId] = mutableListOf()
        }
        return if (userId in blackListSubjectToUser[commandId]!![groupId]!!)
            false
        else {
            blackListSubjectToUser[commandId]!![groupId]!!.add(userId)
            true
        }
    }

    fun removeBlackUserInGroup(groupId: Long, userId: Long): Boolean {
        if (blackListSubjectToUser[commandId] == null) {
            blackListSubjectToUser[commandId] = mutableMapOf()
        }
        if (blackListSubjectToUser[commandId]!![groupId] == null) {
            blackListSubjectToUser[commandId]!![groupId] = mutableListOf()
        }
        return if (userId !in blackListSubjectToUser[commandId]!![groupId]!!)
            false
        else {
            blackListSubjectToUser[commandId]!![groupId]!!.remove(userId)
            true
        }
    }

    fun addCall(call: Call): Boolean {
        callHistory[call.commandId]?.add(call) ?: kotlin.run {
            callHistory[call.commandId] = mutableListOf(call)
        }
        Rika.logger.debug("$call added.")
        return true
    }
    fun removeCall(call: Call): Boolean {
        callHistory[call.commandId]?.remove(call) ?: kotlin.run {
            callHistory[call.commandId] = mutableListOf(call)
        }
        Rika.logger.debug("$call removed.")
        return true
    }

    fun canCall(call: Call): Boolean {
        val check1 = hasPermission(call)
        val check2 = checkFrequency(call)
        val check3 = checkCountLimit(call)
        val and = check1.and(check2).and(check3)
        if (!and)
            Rika.logger.debug("pre check failed.")
        return and
    }

    fun hasPermission(call: Call): Boolean {
        if (tempDisable[call.commandId]?.contains(call.subjectId) == true) {
            Rika.logger.debug("permission check failed, command is disabled for subject(${call.subjectId}).")
            return false
        }
        if (!defaultEnable) {
            Rika.logger.debug("permission check failed, command disabled.")
            return false
        }
        call.subjectId ?: return true
        call.userId ?: return true
        val globalMode = when (blockMode[call.commandId]?.get(0)) {
            BLACKLIST -> {
                blackListCheck(call)
            }
            WHITELIST -> {
                whiteListCheck(call)
            }
            EMPTY -> {
                blockMode[call.commandId]?.put(call.subjectId, BLACKLIST) ?: kotlin.run {
                    blockMode[call.commandId] =
                        mutableMapOf(call.subjectId to BLACKLIST)
                }
                Rika.logger.debug("reset blockMode(global) to BLACKLIST.")
                true
            }
            null -> {
                //"blockMode(global) is null, ignore."
                true
            }
        }
        if (!globalMode) {
            Rika.logger.debug("global permission check filed.")
            return false
        }
        val subjectMode = when (blockMode[call.commandId]?.get(call.subjectId)) {
            BLACKLIST -> {
                blackListCheck(call)
            }
            WHITELIST -> {
                whiteListCheck(call)
            }
            EMPTY -> {
                blockMode[call.commandId]?.put(call.subjectId, BLACKLIST)
                    ?: kotlin.run {
                        blockMode[call.commandId] =
                            mutableMapOf(call.subjectId to BLACKLIST)
                    }
                Rika.logger.debug("reset block mode to BLACKLIST.")
                true
            }
            null -> {
                Rika.logger.debug("blockMode is null,set to BLACKLIST for subject.")
                if (blockMode[call.commandId] == null)
                    blockMode[call.commandId] = mutableMapOf()
                blockMode[call.commandId]!![call.subjectId] = BLACKLIST
                blackListCheck(call)
            }
        }
        if (!subjectMode)
            Rika.logger.debug("subject permission check filed.")
        return subjectMode
    }

    fun addBlackGroup(id: Long) {
        blackListSubject[commandId]?.add(id) ?: kotlin.run {
            blackListSubject[commandId] = mutableListOf(id)
        }
    }

    fun removeBlackGroup(id: Long) {
        blackListSubject[commandId]?.remove(id)
    }

    @OptIn(ConsoleExperimentalApi::class)
    fun checkCountLimit(call: Call): Boolean {
        val finalLimit = getCountLimit(call)

        val size = getCallSizeOrNull(call)
        size ?: return true
        if (size == finalLimit) {
            Rika.logger.debug("last call, limit: $finalLimit.")
            if (showTip)
                Bot.instances.find { call.subjectId?.let { it1 -> it.getContactOrNull(it1,false) } !=null }?.let {
                    runBlocking{
                        val name = when (defaultCallCountLimitMode) {
                            Global -> "全局"
                            Subject -> "群"
                            User -> "你"
                        }
                        it.getContact(call.subjectId!!,false).sendMessage(At(it.getGroupOrFail(call.subjectId).getOrFail(call.userId!!))+PlainText("他妈！$name${call.commandId}指令的次数已经被你小子用完了($finalLimit)"))
                        delay(1000)
                    }
                }
        }
        if (size > finalLimit) {
            Rika.logger.debug("checkCountLimit failed, limit is $finalLimit.")
            return false
        }
        return true
    }

    fun getCallSizeOrNull(call: Call): Int? {
        val size = when (this.defaultCallCountLimitMode) {
            Global -> callHistory[call.commandId]?.size
            Subject -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId }?.size
            User -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId && it.userId == call.userId }?.size
        }
        return size
    }

    fun checkFrequency(call: Call): Boolean {
        val last = getLastCallOrNull(call)
        last ?: return true
        val minCoolDown = getCoolDown(call.commandId)
        if (call.timeStamp.minus(last.timeStamp) < minCoolDown) {
            Rika.logger.debug("checkFrequency failed, min cd is ${minCoolDown}ms, blockRunMode is ${getBlockRunMode(call.commandId)}.")
            return false
        }
        return true
    }

    private fun getLastCallOrNull(call: Call): Call? {
        val last = when (getBlockRunMode(call.commandId) ?: Subject) {
            Global -> callHistory[call.commandId]?.lastOrNull()
            Subject -> callHistory[call.commandId]?.lastOrNull { it.subjectId == call.subjectId }
            User -> callHistory[call.commandId]?.lastOrNull { it.subjectId == call.subjectId && it.userId == call.userId }
        }
        return last
    }

    fun blackListCheck(call: Call): Boolean {
        if (!blackListGroupCheck(call) || !blackListGroupUserCheck(call) || !blackListGlobalUserCheck(call)) {
            Rika.logger.debug("blackListCheck failed.")
            return false
        }

        return true
    }

    fun whiteListCheck(call: Call): Boolean {
        if (!whiteListGroupCheck(call) || !whiteListGroupUserCheck(call) || !whiteListGlobalUserCheck(call)) {
            Rika.logger.debug("whiteListCheck failed.")
            return false
        }
        return true
    }

    fun blackListGroupCheck(call: Call): Boolean {
        if (call.subjectId in (blackListSubject[call.commandId] ?: mutableListOf())) {
            Rika.logger.debug("blackListGroupCheck failed.")
            return false
        }
        return true
    }

    fun blackListGroupUserCheck(call: Call): Boolean {
        if (call.userId in (blackListSubjectToUser[call.commandId]?.get(call.subjectId) ?: mutableListOf())) {
            Rika.logger.debug("blackListGroupUserCheck failed.")
            return false
        }
        return true
    }

    fun blackListGlobalUserCheck(call: Call): Boolean {
        if (call.userId in (blackListSubjectToUser[call.commandId]?.get(0) ?: mutableListOf())) {
            Rika.logger.debug("blackListGlobalUserCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGroupCheck(call: Call): Boolean {
        if (call.subjectId !in (whiteListSubject[call.commandId] ?: mutableListOf())) {
            Rika.logger.debug("whiteListGroupCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGroupUserCheck(call: Call): Boolean {
        if (call.userId !in (whiteListUser[call.commandId]?.get(call.subjectId) ?: mutableListOf())) {
            Rika.logger.debug("whiteListGroupUserCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGlobalUserCheck(call: Call): Boolean {
        if (call.userId !in (whiteListUser[call.commandId]?.get(0) ?: mutableListOf())) {
            Rika.logger.debug("whiteListGlobalUserCheck failed.")
            return false
        }
        return true
    }
}

private fun File.readText0(): String {
    Rika.logger.debug{"加载配置$name"}
    return readText()
}
