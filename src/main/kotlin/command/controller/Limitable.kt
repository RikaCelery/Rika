package org.celery.command.controller

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.serializersModuleOf
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
import org.celery.config.AutoReloadMap
import org.celery.config.AutoReloadable
import org.celery.config.main.CallConfig
import org.celery.config.main.Keys
import org.celery.config.main.PublicConfig
import org.celery.exceptions.CommandAbortException
import java.io.File
import java.util.*

/**
 * 根据[commandId]控制调用频率和调用次数
 */
interface Limitable {
    companion object {
        val allRegistered = mutableListOf<Limitable>()

        //调用历史记录
//        var callHistory = Hashtable<String, MutableList<Call>>()
        object CallHistory : AutoReloadable("limit/CallHistory"), MutableList<Call> {
            override val modules: MutableList<SerializersModule>
                get() = mutableListOf(serializersModuleOf(Call.serializer()).also { println("add module") })

            private var callHistory = mutableListOf<Call>()

            override fun load() {
                callHistory = jsonSerializer.decodeFromString(pluginConfigsFile.readText())
            }

            override fun save() {
                pluginConfigsFile.writeText(
                    jsonSerializer.encodeToString(
                        ListSerializer(Call.serializer()),
                        callHistory
                    )
                )
            }

            operator fun get(commandId: String): List<Call> {
                return callHistory.filter { it.commandId == commandId }
            }

            operator fun get(commandId: String, subjectId: Long?, userId: Long?): List<Call> {
                return callHistory.filter { it.commandId == commandId && it.subjectId == subjectId && it.userId == userId }
            }

            operator fun get(call: Call): List<Call> {
                return callHistory.filter { it.commandId == call.commandId && it.subjectId == call.subjectId && it.userId == call.userId }
            }

            fun filterSubject(commandId: String, id: Long?): List<Call> {
                return callHistory.filter { it.commandId == commandId && it.subjectId == id }
            }

            fun filterSubject(call: Call): List<Call> {
                return callHistory.filter { it.commandId == call.commandId && it.subjectId == call.subjectId }
            }

            fun filterPureUser(commandId: String, id: Long?): List<Call> {
                return callHistory.filter { it.commandId == commandId && it.userId == id }
            }

            fun filterPureUser(call: Call): List<Call> {
                return callHistory.filter { it.commandId == call.commandId && it.userId == call.userId }
            }

            override val size: Int = callHistory.size

            override fun clear() = callHistory.clear()

            override fun addAll(elements: Collection<Call>): Boolean = callHistory.addAll(elements)

            override fun addAll(index: Int, elements: Collection<Call>): Boolean = callHistory.addAll(index, elements)

            override fun add(index: Int, element: Call) = callHistory.add(index, element)

            override fun add(element: Call): Boolean = callHistory.add(element)

            override fun get(index: Int): Call = callHistory.get(index)

            override fun isEmpty(): Boolean = callHistory.isNotEmpty()

            override fun iterator(): MutableIterator<Call> = callHistory.iterator()

            override fun listIterator(): MutableListIterator<Call> = callHistory.listIterator()

            override fun listIterator(index: Int): MutableListIterator<Call> = callHistory.listIterator(index)

            override fun removeAt(index: Int): Call = callHistory.removeAt(index)

            override fun subList(fromIndex: Int, toIndex: Int): MutableList<Call> =
                callHistory.subList(fromIndex, toIndex)

            override fun set(index: Int, element: Call): Call = callHistory.set(index, element)

            override fun retainAll(elements: Collection<Call>): Boolean = callHistory.retainAll(elements)

            override fun removeAll(elements: Collection<Call>): Boolean = callHistory.retainAll(elements)

            override fun remove(element: Call): Boolean = callHistory.remove(element)

            override fun lastIndexOf(element: Call): Int = callHistory.lastIndexOf(element)

            override fun indexOf(element: Call): Int = callHistory.indexOf(element)

            override fun containsAll(elements: Collection<Call>): Boolean = callHistory.containsAll(elements)

            override fun contains(element: Call): Boolean = callHistory.contains(element)
        }

        object ConfigEnableMap : AutoReloadMap("limit/EnableMap")

        // 权限管理(黑白名单)
        object ConfigBlockMode : AutoReloadMap("limit/BlockMode")
        object ConfigBlackListSubject : AutoReloadMap("limit/BlackListSubject")
        object ConfigBlackListSubjectToUser : AutoReloadMap("limit/BlackListSubjectToUser")
        object ConfigWhiteListSubject : AutoReloadMap("limit/WhiteListSubject")
        object ConfigWhiteListSubjectToUser : AutoReloadMap("limit/WhiteListSubjectToUser")

        // 调用限制信息
        object ConfigCallCountLimitSubjectMap : AutoReloadMap("limit/CallCountLimitSubjectMap")
        object ConfigCallCountLimitSubjectToUserMap : AutoReloadMap("limit/CallCountLimitSubjectToUserMap")

        object ConfigLimitConfigKeys {
            const val blockMode = "blockMode"
//            const val
//            const val
//            const val
//            const val
//            const val
        }

        // 暂时关闭的群
        var tempDisable_WhiteList = Hashtable<String, MutableList<Long>>()
        var tempDisable_BlackList = Hashtable<String, MutableList<Long>>()

        object TempDisableWhiteList : AutoReloadMap("limit/TempDisableWhiteList")
        object TempDisableBlackList : AutoReloadMap("limit/TempDisableBlackList")
//        private var enableMap = Hashtable<String, Boolean>()

        // 权限管理(黑白名单)
//        var blockMode = Hashtable<String, MutableMap<Long, CommandBlockMode>>()
//        var blackListSubject = Hashtable<String, MutableList<Long>>()
//        var blackListSubjectToUser = Hashtable<String, MutableMap<Long, MutableList<Long>>>()
//        var whiteListSubject = Hashtable<String, MutableList<Long>>()
//        var whiteListUser = Hashtable<String, MutableMap<Long, MutableList<Long>>>()

        // 调用限制信息
//        var callCountLimitMap = Hashtable<String, Int>()
//        var callCountLimitSubjectMap = Hashtable<String, MutableMap<Long, Int>>()
//        var callCountLimitSubjectToUserMap = Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>()

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

//        /**
//         * 用于从文件中加载共用数据
//         */
//        private fun load() {
//            callHistory = jsonSerializer.decodeFromString<Map<String, MutableList<Call>>>(callHistoryFile.readText0())
//                .toHashTable()
//            tempDisable =
//                (jsonSerializer.decodeFromString<MutableMap<String, MutableList<Long>>>(tempDisableFile.readText0())).toHashTable()
//
//            callCountLimitMap =
//                jsonSerializer.decodeFromString<Map<String, Int>>(callCountLimitMapFile.readText0()).toHashTable()
//            callCountLimitSubjectMap =
//                jsonSerializer.decodeFromString<Map<String, Map<Long, Int>>>(callCountLimitSubjectMapFile.readText0())
//                    .mapValues { it.value.toHashTable() }.toHashTable()
//            callCountLimitSubjectToUserMap =
//                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableMap<Long, Int>>>>(
//                    callCountLimitSubjectToUserMapFile.readText0()
//                ).toHashTable()
//
//            blockMode =
//                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, CommandBlockMode>>>(blockModeFile.readText0())
//                    .toHashTable()
//            blackListSubject =
//                jsonSerializer.decodeFromString<Map<String, MutableList<Long>>>(blackListSubjectFile.readText0())
//                    .toHashTable()
//            blackListSubjectToUser =
//                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableList<Long>>>>(blackListUserFile.readText0())
//                    .toHashTable()
//            whiteListSubject =
//                jsonSerializer.decodeFromString<Map<String, MutableList<Long>>>(whiteListSubjectFile.readText0())
//                    .toHashTable()
//            whiteListUser =
//                jsonSerializer.decodeFromString<Map<String, MutableMap<Long, MutableList<Long>>>>(whiteListUserFile.readText0())
//                    .toHashTable()
//
//            blockRunMode =
//                jsonSerializer.decodeFromString<Map<String, BlockRunMode>>(blockRunModeFile.readText0()).toHashTable()
//            enableMap = jsonSerializer.decodeFromString<Map<String, Boolean>>(enableMapFile.readText0()).toHashTable()
//        }
//
//        /**
//         * 用于将共用数据保存至文件
//         */
//        fun save() {
//            callHistoryFile.writeText(jsonSerializer.encodeToString(callHistory.toMap()))
//            tempDisableFile.writeText(jsonSerializer.encodeToString(tempDisable.toMap()))
//
//            callCountLimitMapFile.writeText(jsonSerializer.encodeToString(callCountLimitMap.toMap()))
//            callCountLimitSubjectMapFile.writeText(jsonSerializer.encodeToString(callCountLimitSubjectMap.toMap()))
//            callCountLimitSubjectToUserMapFile.writeText(jsonSerializer.encodeToString(callCountLimitSubjectToUserMap.toMap()))
//
//            blockModeFile.writeText(jsonSerializer.encodeToString(blockMode.toMap()))
//            blackListSubjectFile.writeText(jsonSerializer.encodeToString(blackListSubject.toMap()))
//            blackListUserFile.writeText(jsonSerializer.encodeToString(blackListSubjectToUser.toMap()))
//            whiteListSubjectFile.writeText(jsonSerializer.encodeToString(whiteListSubject.toMap()))
//            whiteListUserFile.writeText(jsonSerializer.encodeToString(whiteListUser.toMap()))
//
//            blockRunModeFile.writeText(jsonSerializer.encodeToString(blockRunMode.toMap()))
////            blockUserFile.writeText(jsonSerializer.encodeToString(blockUser.toMap()))
////            blockSubjectFile.writeText(jsonSerializer.encodeToString(blockSubject.toMap()))
////            blockGlobalFile.writeText(jsonSerializer.encodeToString(blockGlobal.toMap()))
//
//
//            enableMapFile.writeText(jsonSerializer.encodeToString<Map<String, Boolean>>(enableMap))
//        }
//
//        fun reload() {
//            try {
//                load()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Rika.configFolder.resolve("limitation").listFiles()?.forEach(File::deleteRecursively)
//                save()
//            }
//        }

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
        when (getSubjectBlockMode(subject)) {
            BLACKLIST -> {
                if (TempDisableWhiteList[commandId, mutableListOf<Long>()].contains(subject)) return false
                val list = TempDisableWhiteList[commandId, mutableListOf<Long>()]
                list.add(subject)
                TempDisableWhiteList[commandId] = list
                return true
            }
            WHITELIST -> {
                if (!TempDisableWhiteList[commandId, mutableListOf<Long>()].contains(subject)) return false
                val list = TempDisableWhiteList[commandId, mutableListOf<Long>()]
                list.remove(subject)
                TempDisableWhiteList[commandId] = list
                return true
            }
            EMPTY -> {
                val map = ConfigBlockMode[commandId, mutableMapOf<Long, CommandBlockMode>()]
                Rika.logger.debug("blockMode reset to default(${CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]}) for subject.")
                map[subject] = CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]
                ConfigBlockMode[commandId] = map
                throw IllegalStateException("状态错误，已重置")
            }
        }
    }

    fun enableFor(subject: Long): Boolean {
        when (getSubjectBlockMode(subject)) {
            BLACKLIST -> {
                if (!TempDisableWhiteList[commandId, mutableListOf<Long>()].contains(subject)) return false
                val list = TempDisableWhiteList[commandId, mutableListOf<Long>()]
                list.remove(subject)
                TempDisableWhiteList[commandId] = list
                return true
            }
            WHITELIST -> {
                if (TempDisableWhiteList[commandId, mutableListOf<Long>()].contains(subject)) return false
                val list = TempDisableWhiteList[commandId, mutableListOf<Long>()]
                list.add(subject)
                TempDisableWhiteList[commandId] = list
                return true
            }
            EMPTY -> {
                val map = ConfigBlockMode[commandId, mutableMapOf<Long, CommandBlockMode>()]
                Rika.logger.debug("blockMode reset to default(${CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]}) for subject.")
                map[subject] = CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]
                ConfigBlockMode[commandId] = map
                throw IllegalStateException("状态错误，已重置")
            }
        }
    }

    fun getEnable(): Boolean {
        return defaultEnable
    }

    fun disable() {
        Rika.logger.debug("$commandId disabled.")
        ConfigEnableMap[commandId] = false
    }

    fun enable() {
        Rika.logger.debug("$commandId enabled.")
        ConfigEnableMap[commandId] = true
    }

    /**
     * 用于唯一确定一个指令的名字
     */
    val commandId: String

    /**
     * 控制命令的启用或关闭（全局）
     */
    var defaultEnable: Boolean
        get() = CallConfig["$commandId.enable", true]
        set(value) {
            CallConfig["$commandId.enable"] = value
        }

    /**
     * 默认的最多调用次数,在[callCountLimitMap]中不存在对应值时取这个
     */
    var defaultCountLimit: Int
        get() = CallConfig["$commandId.max_count", PublicConfig[Keys.MAX_COUNT_DEFAULT, 3000]]
        set(value) {
            CallConfig["$commandId.max_count"] = value
        }

    /**
     * 阻塞调用方式
     */
    var defaultCallCountLimitMode: BlockRunMode
        get() = CallConfig["$commandId.block_run_mode", BlockRunMode.User]
        set(value) {
            CallConfig["$commandId.block_run_mode"] = value
        }

    /**
     * 阻塞调用方式,固定不可变
     */
    var defaultBlockRunModeMode: BlockRunMode
        get() = CallConfig["$commandId.block_run_mode", BlockRunMode.Subject]
        set(value) {
            CallConfig["$commandId.block_run_mode"] = value
        }

    /**
     * 默认的冷却时长,在[]中不存在对应值时取这个 毫秒
     */
    var defaultCoolDown: Long
        get() = CallConfig["$commandId.cool_down", PublicConfig[Keys.COOL_DOWN_DEFAULT, 3000]]
        set(value) {
            CallConfig["$commandId.cool_down"] = value
        }

    var showTip: Boolean
        get() = CallConfig["$commandId.show_tip", PublicConfig[Keys.SHOW_TIP, false]]
        set(value) {
            CallConfig["$commandId.show_tip"] = value
        }


    fun getBlockRunMode(commandId: String) = blockRunMode[commandId] ?: this.defaultBlockRunModeMode
    fun getCoolDown(commandId: String) = defaultCoolDown
    fun getCallSize(call: Call) = getCallSizeOrNull(call) ?: 0
    fun getCountLimit(call: Call): Int = getCountLimit(call.commandId, call.subjectId, call.userId)
    fun getCountLimit(id: String, groupId: Long?, userId: Long?): Int {
        val limitable = allRegistered.singleOrNull { it.commandId == id }
        if (limitable == null) {
            println(id)
            return -2
        }
        val userLimit =
            ConfigCallCountLimitSubjectToUserMap[id, mutableMapOf<Long, MutableMap<Long, Int>>()][groupId]?.get(userId)
        if (userLimit != null) return userLimit
        val groupLimit = ConfigCallCountLimitSubjectMap[id, mutableMapOf<Long, Int>()][groupId]
        if (groupLimit != null) return groupLimit
        return limitable.defaultCountLimit
    }

    fun getCallSizeOrNull(call: Call, mode: BlockRunMode): Int? {
        val size = when (mode) {
            Global -> CallHistory[call.commandId].size
            Subject -> CallHistory.filterSubject(call).size
            User -> CallHistory[call].size
            PureUser -> CallHistory.filterPureUser(call).size
        }
        return size
    }

    fun setUserCountLimit(subject: Long, userId: Long, limit: Int) {
        val map = ConfigCallCountLimitSubjectToUserMap[commandId, mutableMapOf<Long, MutableMap<Long, Int>>()]
        if (map[subject] == null) map[subject] = mutableMapOf()
        map[subject]!![userId] = limit
        ConfigCallCountLimitSubjectToUserMap[commandId] = map
    }

    fun getUserCountLimit(subject: Long, userId: Long): Int? {
        return ConfigCallCountLimitSubjectToUserMap[commandId, mutableMapOf<Long, MutableMap<Long, Int>>()][subject]?.get(
            userId
        )
    }

    fun setGroupCountLimit(subject: Long, limit: Int) {
        val map = ConfigCallCountLimitSubjectMap[commandId, mutableMapOf<Long, Int>()]
        map[subject] = limit
        ConfigCallCountLimitSubjectMap[commandId] = map
    }

    fun getGroupCountLimit(subject: Long): Int? {
        return ConfigCallCountLimitSubjectMap[commandId, mutableMapOf<Long, Int>()][subject]
    }

    fun setGlobalLimit(limit: Int) {
        defaultCountLimit = limit
    }

    fun getGlobalLimit(): Int? = defaultCountLimit

    fun addBlackUserGlobal(userId: Long): Boolean {
        return addBlackUserInGroup(0, userId)
    }

    fun removeBlackUserGlobal(userId: Long): Boolean {
        return removeBlackUserInGroup(0, userId)
    }

    fun addBlackUserInGroup(groupId: Long, userId: Long): Boolean {
        return if (userId in (ConfigBlackListSubjectToUser[commandId, mutableMapOf<Long, MutableList<Long>>()][groupId]
                ?: mutableListOf())
        ) false
        else {
            ConfigBlackListSubjectToUser[commandId] =
                ConfigBlackListSubjectToUser[commandId, mutableMapOf<Long, MutableList<Long>>()][groupId]!!.add(userId)
            true
        }
    }

    fun removeBlackUserInGroup(groupId: Long, userId: Long): Boolean {
        return if (userId !in (ConfigBlackListSubjectToUser[commandId, mutableMapOf<Long, MutableList<Long>>()][groupId]
                ?: mutableListOf())
        ) false
        else {
            ConfigBlackListSubjectToUser[commandId] =
                ConfigBlackListSubjectToUser[commandId, mutableMapOf<Long, MutableList<Long>>()][groupId]!!.remove(
                    userId
                )
            true
        }
    }

    fun addCall(call: Call): Boolean {
        CallHistory.add(call)
        Rika.logger.debug("$call added.")
        return true
    }

    fun removeCall(call: Call): Boolean {
        CallHistory.remove(call)
        Rika.logger.debug("$call removed.")
        return true
    }

    fun canCall(call: Call): Boolean {
        val check1 = hasPermission(call)
        val check2 = checkFrequency(call)
        val check3 = checkCountLimit(call)
        val and = check1.and(check2).and(check3)
        if (!and) Rika.logger.debug("pre check failed.")
        return and
    }

    fun hasPermission(call: Call): Boolean {
        if (tempDisable_WhiteList[call.commandId]?.contains(call.subjectId) == true) {
            Rika.logger.debug("permission check failed, command is disabled for subject(${call.subjectId}).")
            return false
        }
        if (!(ConfigEnableMap.getOrNull<Boolean>(commandId) ?: defaultEnable)) {
            Rika.logger.debug("permission check failed, command disabled.")
            return false
        }
        call.subjectId ?: return true
        call.userId ?: return true
        val map = ConfigBlockMode[commandId, mutableMapOf<Long, CommandBlockMode>()]
        val globalMode = when (map[0]) {
            BLACKLIST -> {
                blackListCheck(call)
            }
            WHITELIST -> {
                whiteListCheck(call)
            }
            EMPTY -> {
                map.put(0, BLACKLIST)
                ConfigBlockMode[call.commandId] = map
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
        val subjectMode = when (map.get(call.subjectId)) {
            BLACKLIST -> {
                blackListCheck(call)
            }
            WHITELIST -> {
                whiteListCheck(call)
            }
            EMPTY -> {
                map.put(call.subjectId, BLACKLIST)
                ConfigBlockMode[call.commandId] = map
                Rika.logger.debug("reset block mode to BLACKLIST.")
                true
            }
            null -> {
                val default = CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]
                Rika.logger.debug("blockMode is null,set to default($default) for subject.")
                map[call.subjectId] = default
                ConfigBlockMode[call.commandId] = map
                if (default == BLACKLIST)
                    blackListCheck(call)
                else if (default == WHITELIST)
                    whiteListCheck(call)
                else
                    throw IllegalStateException("line 585: neither BLACKLIST or WHITELIST")
            }
        }
        if (!subjectMode) Rika.logger.debug("subject permission check filed.")
        return subjectMode
    }

    fun getGlobalBlockMode(commandId: String = this.commandId) =
        ConfigBlockMode[commandId, mutableMapOf<Long, CommandBlockMode>()][0]

    fun getSubjectBlockMode(subjectId: Long, commandId: String = this.commandId): CommandBlockMode {
        val map = ConfigBlockMode[commandId, mutableMapOf<Long, CommandBlockMode>()]
        if (map[subjectId] == null) {

            Rika.logger.debug("blockMode is null,set to default(${CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]}) for subject.")
            map[subjectId] = CallConfig[Keys.DEFAULT_BLOCK_MODE, BLACKLIST]
            ConfigBlockMode[commandId] = map

        }
        return map[subjectId]!!
    }

    fun addBlackGroup(id: Long) {
        ConfigBlackListSubject[commandId] = ConfigBlackListSubject[commandId, mutableListOf(id)].add(id)
    }

    fun removeBlackGroup(id: Long) {
        ConfigBlackListSubject[commandId] = ConfigBlackListSubject[commandId, mutableListOf(id)].remove(id)
    }

    @OptIn(ConsoleExperimentalApi::class)
    fun checkCountLimit(call: Call): Boolean {
        val finalLimit = getCountLimit(call)

        val size = getCallSizeOrNull(call)
        size ?: return true
        if (finalLimit == -1) {
            return true
        }
        if (size == finalLimit) {
            Rika.logger.debug("last call, limit: $finalLimit.")
            try {
                if (showTip) runBlocking {
                    Bot.instances.find { call.subjectId?.let { it1 -> it.getContactOrNull(it1, false) } != null }
                        ?.limitNotice(call, finalLimit)
                }
            } catch (e: CommandAbortException) {
                Rika.logger.debug("abort.")
                return false
            }
        }
        if (size > finalLimit) {
            Rika.logger.debug("checkCountLimit failed, limit is $finalLimit.")
            return false
        }
        return true
    }

    val limitMessage: String?

    @OptIn(ConsoleExperimentalApi::class)
    suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {
        runBlocking {
            val name = when (this@Limitable.defaultCallCountLimitMode) {
                Global -> "全局"
                Subject -> "群"
                User -> "你群的你"
                PureUser -> "你"
            }
            if (limitMessage != null) {
                getContact(call.subjectId!!, false).sendMessage(limitMessage!!)
            } else getContact(call.subjectId!!, false).sendMessage(
                At(
                    getGroupOrFail(call.subjectId).getOrFail(call.userId!!)
                ) + PlainText("他妈！${name}的${call.commandId}指令的次数已经被你小子用完了($finalLimit)")
            )
            delay(1000)
        }

    }

    fun getCallSizeOrNull(call: Call): Int? {
        val size = when (defaultCallCountLimitMode) {
            Global -> CallHistory[call.commandId].size
            Subject -> CallHistory.filterSubject(call).size
            User -> CallHistory[call].size
            PureUser -> CallHistory.filterPureUser(call).size
        }
        return size.let { if (it == 0) null else it }
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
        val last = when (getBlockRunMode(call.commandId)) {
            Global -> CallHistory[call.commandId].maxByOrNull { it.timeStamp }
            Subject -> CallHistory.filterSubject(call).maxByOrNull { it.timeStamp }
            User -> CallHistory[call].maxByOrNull { it.timeStamp }
            PureUser -> CallHistory.filterPureUser(call).maxByOrNull { it.timeStamp }
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
        if (whiteListGroupCheck(call) || whiteListGroupUserCheck(call) || whiteListGlobalUserCheck(call)) {
            return true
        }
        Rika.logger.debug("whiteListCheck failed.")
        return false
    }

    fun blackListGroupCheck(call: Call): Boolean {
        if (call.subjectId in (ConfigBlackListSubject[commandId, mutableListOf<Long>()])) {
            Rika.logger.debug("blackListGroupCheck failed.")
            return false
        }
        return true
    }

    fun blackListGroupUserCheck(call: Call): Boolean {
        if (call.userId in (ConfigBlackListSubjectToUser[call.commandId, mutableMapOf<Long, MutableList<Long>>()][call.subjectId]
                ?: mutableListOf())
        ) {
            Rika.logger.debug("blackListGroupUserCheck failed.")
            return false
        }
        return true
    }

    fun blackListGlobalUserCheck(call: Call): Boolean {
        if (call.userId in (ConfigBlackListSubjectToUser[call.commandId, mutableMapOf<Long, MutableList<Long>>()][0]
                ?: mutableListOf())
        ) {
            Rika.logger.debug("blackListGlobalUserCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGroupCheck(call: Call): Boolean {
        if (call.subjectId !in (ConfigWhiteListSubject[call.commandId, mutableListOf<Long>()])) {
            Rika.logger.debug("whiteListGroupCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGroupUserCheck(call: Call): Boolean {
        if (call.userId !in (ConfigWhiteListSubjectToUser[call.commandId, mutableMapOf<Long, MutableList<Long>>()][call.subjectId]
                ?: mutableListOf())
        ) {
            Rika.logger.debug("whiteListGroupUserCheck failed.")
            return false
        }
        return true
    }

    fun whiteListGlobalUserCheck(call: Call): Boolean {
        if (call.userId !in (ConfigWhiteListSubjectToUser[call.commandId, mutableMapOf<Long, MutableList<Long>>()][0]
                ?: mutableListOf())
        ) {
            Rika.logger.debug("whiteListGlobalUserCheck failed.")
            return false
        }
        return true
    }
}

private fun File.readText0(): String {
    Rika.logger.debug { "加载配置$name" }
    return readText()
}
