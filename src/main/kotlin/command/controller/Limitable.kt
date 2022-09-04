package org.celery.command.controller

import org.celery.command.controller.BlockRunMode.*
import org.celery.command.controller.CommandBlockMode.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.celery.Rika
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 根据[commandId]控制调用频率和调用次数
 */
interface Limitable {
    companion object {
        //调用历史记录
        var callHistory = Hashtable<String, MutableList<Call>>()
        // 暂时关闭的群
        var tempDisable = ConcurrentLinkedDeque<Long>()
        // 权限管理(黑白名单)
        var blockMode = Hashtable<String, MutableMap<Long, CommandBlockMode>>()
        var blackListSubject = Hashtable<String, MutableList<Long>>()
        var blackListUser = Hashtable<String, MutableMap<Long, List<Long>>>()
        var whiteListSubject = Hashtable<String, MutableList<Long>>()
        var whiteListUser = Hashtable<String, MutableMap<Long, List<Long>>>()

        // 调用限制信息
        var callCountLimitMap = Hashtable<String, Int>()
        var callCountLimitSubjectMap = Hashtable<String, MutableMap<Long, Int>>()
        var callCountLimitSubjectToUserMap = Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>()

        // 调用限制
        var blockRunMode = Hashtable<String, BlockRunMode>()
        var blockUser = Hashtable<String, MutableList<Long?>>()
        var blockSubject = Hashtable<String, MutableList<Long?>>()
        var blockGlobal = Hashtable<String, Boolean>()//FIXME: 其实这个地方放个HashSet就够了

        // json序列化器
        private val jsonSreializer = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            serializersModule += SerializersModule {
                Call.serializer()
            }
        }

        /**
         * 用于从文件中加载共用数据
         */
        fun load() {
//          `  val callHistoryFile = Rika.configFolder.resolve("limitation/callHistory.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val defaultCallCountLimitFile = Rika.configFolder.resolve("limitation/defaultCallCountLimit.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val callLimitSubjectFile = Rika.configFolder.resolve("limitation/callLimitSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val callLimitSubjectToUserFile = Rika.configFolder.resolve("limitation/callLimitSubjectToUser.json")
//                .apply { if `(parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockModeFile = Rika.configFolder.resolve("limitation/blockMode.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blackListSubjectFile = Rika.configFolder.resolve("limitation/blackListSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blackListUserFile = Rika.configFolder.resolve("limitation/blackListUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val whiteListSubjectFile = Rika.configFolder.resolve("limitation/whiteListSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val whiteListUserFile = Rika.configFolder.resolve("limitation/whiteListUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockRunModeFile = Rika.configFolder.resolve("limitation/blockRunMode.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockUserFile = Rika.configFolder.resolve("limitation/blockUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockSubjectFile = Rika.configFolder.resolve("limitation/blockSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockGlobalFile = Rika.configFolder.resolve("limitation/blockGlobal.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            callHistory =
//                jsonSreializer.decodeFromString<Map<String, MutableList<Call>>>(callHistoryFile.readText())
//                    .toHashTable()
//            callCountLimitMap =
//                jsonSreializer.decodeFromString<Map<String, Int>>(defaultCallCountLimitFile.readText()).toHashTable()
//            callLimitSubjectMap =
//                jsonSreializer.decodeFromString<Map<String, Map<Long, Int>>>(callLimitSubjectFile.readText())
//                    .mapValues { it.value.toHashTable() }.toHashTable()
//            callLimitSubjectToUserMap =
//                jsonSreializer.decodeFromString<Map<String, MutableMap<Long, MutableMap<Long, Int>>>>(
//                    callLimitSubjectToUserFile.readText()
//                ).toHashTable()
//            blockMode =
//                jsonSreializer.decodeFromString<Map<String, MutableMap<Long, Command.CommandBlockMode>>>(blockModeFile.readText())
//                    .toHashTable()
//            blackListSubject =
//                jsonSreializer.decodeFromString<Map<String, MutableList<Long>>>(blackListSubjectFile.readText())
//                    .toHashTable()
//            blackListUser =
//                jsonSreializer.decodeFromString<Map<String, MutableMap<Long, List<Long>>>>(blackListUserFile.readText())
//                    .toHashTable()
//            whiteListSubject =
//                jsonSreializer.decodeFromString<Map<String, MutableList<Long>>>(whiteListSubjectFile.readText())
//                    .toHashTable()
//            whiteListUser =
//                jsonSreializer.decodeFromString<Map<String, MutableMap<Long, List<Long>>>>(whiteListUserFile.readText())
//                    .toHashTable()
//            blockRunMode =
//                jsonSreializer.decodeFromString<Map<String, Command.BlockRunMode>>(blockRunModeFile.readText())
//                    .toHashTable()
//            blockUser =
//                jsonSreializer.decodeFromString<Map<String, MutableList<Long?>>>(blockUserFile.readText()).toHashTable()
//            blockSubject =
//                jsonSreializer.decodeFromString<Map<String, MutableList<Long?>>>(blockSubjectFile.readText())
//                    .toHashTable()
//            blockGlobal =
//                jsonSreializer.decodeFromString<Map<String, Boolean>>(blockGlobalFile.readText()).toHashTable()
        }

        /**
         * 用于将共用数据保存至文件
         */
        fun save() {
//            val callHistoryFile = Rika.configFolder.resolve("limitation/callHistory.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val defaultCallCountLimitFile = Rika.configFolder.resolve("limitation/defaultCallCountLimit.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val callLimitSubjectFile = Rika.configFolder.resolve("limitation/callLimitSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val callLimitSubjectToUserFile = Rika.configFolder.resolve("limitation/callLimitSubjectToUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockModeFile = Rika.configFolder.resolve("limitation/blockMode.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blackListSubjectFile = Rika.configFolder.resolve("limitation/blackListSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blackListUserFile = Rika.configFolder.resolve("limitation/blackListUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val whiteListSubjectFile = Rika.configFolder.resolve("limitation/whiteListSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val whiteListUserFile = Rika.configFolder.resolve("limitation/whiteListUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockRunModeFile = Rika.configFolder.resolve("limitation/blockRunMode.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockUserFile = Rika.configFolder.resolve("limitation/blockUser.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockSubjectFile = Rika.configFolder.resolve("limitation/blockSubject.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            val blockGlobalFile = Rika.configFolder.resolve("limitation/blockGlobal.json")
//                .apply { if (parentFile.exists().not()) parentFile.mkdirs(); if (exists().not()) createNewFile() }
//            callHistoryFile.writeText(jsonSreializer.encodeToString(callHistory.toMap()))
//            defaultCallCountLimitFile.writeText(jsonSreializer.encodeToString(callCountLimitMap.toMap()))
//            callLimitSubjectFile.writeText(jsonSreializer.encodeToString(callLimitSubjectMap.toMap()))
//            callLimitSubjectToUserFile.writeText(jsonSreializer.encodeToString(callLimitSubjectToUserMap.toMap()))
//            blockModeFile.writeText(jsonSreializer.encodeToString(blockMode.toMap()))
//            blackListSubjectFile.writeText(jsonSreializer.encodeToString(blackListSubject.toMap()))
//            blackListUserFile.writeText(jsonSreializer.encodeToString(blackListUser.toMap()))
//            whiteListSubjectFile.writeText(jsonSreializer.encodeToString(whiteListSubject.toMap()))
//            whiteListUserFile.writeText(jsonSreializer.encodeToString(whiteListUser.toMap()))
//            blockRunModeFile.writeText(jsonSreializer.encodeToString(blockRunMode.toMap()))
//            blockUserFile.writeText(jsonSreializer.encodeToString(blockUser.toMap()))
//            blockSubjectFile.writeText(jsonSreializer.encodeToString(blockSubject.toMap()))
//            blockGlobalFile.writeText(jsonSreializer.encodeToString(blockGlobal.toMap()))
        }

        /**
         * Map to HashTable
         */
        private fun <K, V> Map<K, V>.toHashTable(): Hashtable<K, V> {
            val hashtable = Hashtable<K, V>()
            forEach { t, u ->
                hashtable[t] = u
            }
            return hashtable
        }
    }

    fun disableFor(subject:Long): Boolean {
        if (tempDisable.contains(subject))
            return false
        return  tempDisable.add(subject)
    }
    fun enbaleFor(subject:Long): Boolean {
        return(tempDisable.remove(subject))
    }
    fun disable(){
        defultEnable = false
    }
    fun enable(){
        defultEnable = true
    }

    /**
     * 用于唯一确定一个指令的名字
     */
    val commandId: String

    /**
     * 控制命令的启用或关闭（全局）
     */
    var defultEnable: Boolean

    /**
     * 默认的最多调用次数,在[callCountLimitMap]中不存在对应值时取这个
     */
    var defultCountLimit: Int

    /**
     * 阻塞调用方式,固定不可变
     */
    var defultCallCountLimitMode: BlockRunMode

    /**
     * 默认的冷却时长,在[]中不存在对应值时取这个//TODO:添加冷却时长映射
     */
    var defultMinCooldown: Int



    fun getBlockRunMode(commandId: String) = blockRunMode[commandId] ?: defultCallCountLimitMode
    fun getCoolDown(commandId: String) = defultMinCooldown
    fun getCountLimit(commandId: String) = callCountLimitMap[commandId]
    fun getCountLimit(call: Call): Int = getCountLimit(call.commandId,call.subjectId,call.userId)
    fun getCountLimit(id:String,key:Long?,userId:Long?): Int {
        val userLimit = callCountLimitSubjectToUserMap[id]?.get(key)?.get(userId)
        if (userLimit != null)
            return userLimit
        val groupLimit = callCountLimitSubjectMap[id]?.get(key)
        if (groupLimit != null)
            return groupLimit
        val globalLimit = callCountLimitMap[id]
        if (globalLimit != null)
            return globalLimit
        return defultCountLimit
    }


    fun addCall(call: Call): Boolean {
        callHistory[call.commandId]?.add(call) ?: kotlin.run {
            callHistory[call.commandId] = mutableListOf(call)
        }
        Rika.logger.debug("$call added.")
        return true
    }

    fun canCall(call: Call): Boolean {
        if (!defultEnable) {
            Rika.logger.debug("pre check faild, command disabled.")
            return false
        }
        val check1 = hasPermission(call)
        val check2 = checkFrequency(call)
        val check3 = checkCountLimit(call)
        val and = check1.and(check2).and(check3)
        if (!and)
            Rika.logger.debug("pre check faild.")
        return and
    }

    fun hasPermission(call: Call): Boolean {
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
        if (!globalMode)
            return false
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
                false//FIXME 第一次调用没反应
            }
        }
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

    fun checkCountLimit(call: Call): Boolean {
        val finalLimit = getCountLimit(call)

        val size = getCallSizeOrNull(call)
        size ?: return true
        if (size == finalLimit) {
            Rika.logger.debug("last call, limit: $finalLimit.")
            //TODO 最后一次提示
        }
        if (size > finalLimit) {
            Rika.logger.debug("checkCountLimit failed, limit is $finalLimit.")
            return false
        }
        return true
    }

    private fun getCallSizeOrNull(call: Call): Int? {
        val size = when (defultCallCountLimitMode) {
            Global -> callHistory[call.commandId]?.size
            Subject -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId }?.size
            User -> callHistory[call.commandId]?.filter { it.subjectId == call.subjectId && it.userId == call.userId }?.size
        }
        return size
    }

    fun checkFrequency(call: Call): Boolean {
        val last = getLastCallOrNull(call)
        last ?: return true
        val minCooldown = getCoolDown(call.commandId)
        if (call.timeStamp.minus(last.timeStamp) < minCooldown) {
            Rika.logger.debug("checkFrequency failed, min cd is ${minCooldown}ms, blockRunMode is ${blockRunMode[call.commandId]}.")
            return false
        }
        return true
    }

    private fun getLastCallOrNull(call: Call): Call? {
        val last = when (blockRunMode[call.commandId] ?: Subject) {
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
        if (call.userId in (blackListUser[call.commandId]?.get(call.subjectId) ?: mutableListOf())) {
            Rika.logger.debug("blackListGroupUserCheck failed.")
            return false
        }
        return true
    }

    fun blackListGlobalUserCheck(call: Call): Boolean {
        if (call.userId in (blackListUser[call.commandId]?.get(0) ?: mutableListOf())) {
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