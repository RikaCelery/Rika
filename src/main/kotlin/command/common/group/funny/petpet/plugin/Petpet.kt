package org.celery.command.common.group.funny.petpet.plugin

import command.common.group.funny.petpet.plugin.DataUpdater
import command.common.group.funny.petpet.plugin.PetPetAutoSaveConfig
import command.common.group.funny.petpet.plugin.PluginPetService
import command.common.group.funny.petpet.plugin.ReplyFormat
import command.common.group.funny.petpet.share.BaseConfigFactory
import command.common.group.funny.petpet.share.TextExtraData
import events.ExecutionResult
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import org.celery.Rika.reload
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import java.util.*
import java.util.stream.Collectors

object Petpet : Command(
    "petpet"
) {

    override fun init() {
        PetPetAutoSaveConfig.reload()
        service.readData(dataFolder)
        if (service.headless) System.setProperty("java.awt.headless", "true")
        if (service.autoUpdate) Thread(DataUpdater::autoUpdate).start()
        disabledGroup = service.disabledGroups?.toMutableList()?: mutableListOf()
        logger.info(
            """
             _                _   
  _ __   ___| |_   _ __   ___| |_ 
 | '_ \ / _ \ __| | '_ \ / _ \ __|
 | |_) |  __/ |_  | |_) |  __/ |_ 
 | .__/ \___|\__| | .__/ \___|\__|
 |_|              |_|             v$VERSION"""
        )

        if (service.respondReply) {
            imageCachePool = object : LinkedHashMap<Long, String?>(service.cachePoolSize, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long?, String?>?): Boolean {
                    return size > service.cachePoolSize
                }
            }
            GlobalEventChannel.subscribeAlways { e: GroupMessageEvent ->
                cacheMessageImage(e)
            }
            GlobalEventChannel.subscribeAlways { e: GroupMessagePostSendEvent? ->
                e?.let {
                    cacheMessageImage(it)
                }
            }
        }
    }

    @Command
    suspend fun GroupMessageEvent.responseGroup(eventMatchResult: EventMatchResult): ExecutionResult {
        eventMatchResult.getdata<suspend ()->Unit>()()
        if (message.content==PetPetAutoSaveConfig.content.command) return ExecutionResult.Ignored
        return ExecutionResult.Success
    }

    @Trigger("responseGroup")
    suspend fun BotEvent.trigger(): EventMatchResult? {
        val func = when (this) {
            is GroupMessageEvent -> {
                if (service.messageSynchronized) onGroupMessageSynchronized(this)
                else onGroupMessage(this)
            }
            is NudgeEvent -> {
                if (service.probability > 0) GlobalEventChannel.subscribeAlways<NudgeEvent> { e ->
                    if (service.messageSynchronized) onNudgeSynchronized(e)
                    else onNudge(e)

                }
                null
            }
            else -> null
        }
        return func?.let { EventMatchResult(data = it) }
    }

    private suspend fun onNudgeSynchronized(e: NudgeEvent): (suspend () -> Unit)? {
        synchronized(this) {
            if (nudgeEventAreEqual(previousNudge, e)) return null
            previousNudge = e
        }
        return responseNudge(e)
    }

    private suspend fun onNudge(e: NudgeEvent) {
        responseNudge(e)
    }

    private suspend fun responseNudge(e: NudgeEvent): (suspend () -> Unit)? {
        // 如果禁用了petpet就返回
        if ((e.subject !is Group || isDisabled(e.subject as Group)) && service.nudgeCanBeDisabled) return null
        try {
            return suspend { service.sendImage(e.subject as Group, e.from as Member, e.target as Member, true) }
        } catch (ex: Exception) { // 如果无法把被戳的对象转换为Member(只有Bot无法强制转换为Member对象)
            try {
                return suspend {
                    service.sendImage(e.subject as Group, (e.subject as Group).botAsMember, e.from as Member, true)
                }
            } catch (ignored: Exception) { // 如果bot戳了别人
                if (!service.respondSelfNudge) return null
                return suspend {
                    service.sendImage(e.subject as Group, (e.subject as Group).botAsMember, e.from as Member, true)
                }
            }
        }
    }

    private suspend fun onGroupMessageSynchronized(e: GroupMessageEvent): (suspend () -> Unit)? {
        synchronized(this) {
            val thisMessageSource: MessageSource = e.message.source
            for (bot in Bot.instances) { // 过滤其它bot发出的消息
                if (previousMessage != null && thisMessageSource.fromId == bot.id) return null
            }
            if (messageSourceAreEqual(previousMessage, thisMessageSource)) return null
            previousMessage = thisMessageSource
        }
        return responseMessage(e)
    }

    private suspend fun onGroupMessage(e: GroupMessageEvent): (suspend () -> Unit)? {
        return responseMessage(e)
    }

    private suspend fun responseMessage(e: GroupMessageEvent): (suspend () -> Unit)? {
        if (!e.message.contains(MessageContent)) return null
        val messageString: String = e.message.contentToString().trim { it <= ' ' }
        if (messageString == service.command + " off" && !isDisabled(e.group) && isPermission(e)) {
            disabledGroup!!.add(e.group.id)
            sendReplyMessage(e, "已禁用 " + service.command)
            return null
        }
        if (messageString == service.command + " on" && isDisabled(e.group) && isPermission(e)) {
            disabledGroup!!.remove(e.group.id)
            sendReplyMessage(e, "已启用 " + service.command)
            return null
        }
        if (service.messageCanBeDisabled && isDisabled(e.group)) return null
        if (messageString == service.command) {
            when (service.replyFormat) {
                ReplyFormat.MESSAGE -> e.group.sendMessage(
                    """
    Petpet KeyList: 
    ${service.keyAliasListString}
    """.trimIndent()
                )
                ReplyFormat.FORWARD -> {
                    val builder = ForwardMessageBuilder(e.group)
                    builder.add(
                        e.bot.id, "petpet!", PlainText(
                            """
    Petpet KeyList: 
    ${service.keyAliasListString}
    """.trimIndent()
                        )
                    )
                    e.group.sendMessage(builder.build())
                }
                ReplyFormat.IMAGE -> {
                    if (service.dataMap.get("key_list") == null) {
                        logger.error("未找到PetData/key_list, 无法进行图片构造")
                        e.group.sendMessage(
                            """
    [ERROR]未找到PetData/key_list
    ${service.keyAliasListString}
    """.trimIndent()
                        )
                        return null
                    }
                    val keyList: MutableList<String> = ArrayList()
                    keyList.add(service.keyAliasListString)
                    return suspend  {
                        service.sendImage(
                            e.group,
                            "key_list",
                            BaseConfigFactory.getGifAvatarExtraDataFromUrls(null, null, null, null),
                            TextExtraData("", "", "", keyList)
                        )
                    }
                }
            }
            return null
        }
        var fuzzyLock = false //锁住模糊匹配
        var fromName = getNameOrNick(e.group.botAsMember)
        var toName: String = e.senderName
        val groupName: String = e.group.name
        val messageText = StringBuilder()
        var fromUrl: String = e.bot.avatarUrl
        var toUrl: String? = e.sender.avatarUrl
        for (singleMessage in e.message) {
            if (singleMessage is QuoteReply && service.respondReply) {
                val id: Long = e.group.id + (singleMessage as QuoteReply).source.ids.get(0)
                toUrl = if (imageCachePool!![id] != null) imageCachePool!![id] else toUrl
                fuzzyLock = true
                continue
            }
            if (singleMessage is PlainText) {
                val text: String = singleMessage.contentToString()
                messageText.append(text).append(' ')
                continue
            }
            if (singleMessage is At && !fuzzyLock) {
                fuzzyLock = true
                val (target) = singleMessage
                if (target == e.sender.id) continue
                fromName = getNameOrNick(e.sender)
                fromUrl = e.sender.avatarUrl
                val to: NormalMember = e.group.get(target)!!
                toName = getNameOrNick(to)
                toUrl = to.avatarUrl
                continue
            }
            if (singleMessage is Image) {
                fromName = getNameOrNick(e.sender)
                fromUrl = e.sender.avatarUrl
                toName = "这个"
                toUrl = singleMessage.queryUrl()
                fuzzyLock = true
            }
        }
        val commandData = messageText.toString().trim { it <= ' ' }
        val spanList = ArrayList(Arrays.asList(*commandData.trim { it <= ' ' }.split("\\s+".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        )
        if (spanList.isEmpty()) return null
        var key: String? = null
        if (service.command.equals(spanList[0])) {
            spanList.removeAt(0) //去掉指令头
            key = service.randomableList.get(Random().nextInt(service.randomableList.size)) //随机key
        }
        if (!spanList.isEmpty() && !service.strictCommand) { //匹配非标准格式指令
            if (keyAliaSet == null) { //按需初始化
                keyAliaSet = HashSet(service.dataMap.keys)
                keyAliaSet!!.addAll(service.aliaMap.keys)
                keyAliaSet =
                    keyAliaSet!!.stream().map { str: String -> service.commandHead + str }.collect(Collectors.toSet())
            }
            for (k in keyAliaSet!!) {
                if (spanList[0].startsWith(k)) {
                    val span = spanList.set(0, k)
                    if (span.length != k.length) {
                        spanList.add(1, span.substring(k.length))
                    }
                }
            }
        }
        if (!spanList.isEmpty()) {
            var firstSpan = spanList[0]
            if (firstSpan.startsWith(service.commandHead)) {
                spanList[0] = firstSpan.substring(service.commandHead.length).also { firstSpan = it }
            } else {
                return null
            }
            if (service.dataMap.containsKey(firstSpan)) { //key
                key = spanList.removeAt(0)
            } else if (service.aliaMap.containsKey(firstSpan)) { //别名
                val keys: Array<String> = service.aliaMap[spanList.removeAt(0)]!!
                key = keys[Random().nextInt(keys.size)]
            }
        }
        if (key == null) return null
        if (service.fuzzy && !spanList.isEmpty() && !fuzzyLock) {
            for (m in e.group.members) {
                if (m.nameCard.lowercase(Locale.getDefault())
                        .contains(spanList[0].lowercase(Locale.getDefault())) || m.nick.lowercase(Locale.getDefault())
                        .contains(spanList[0].lowercase(Locale.getDefault()))
                ) {
                    if (e.sender.id == m.id) break
                    fromName = getNameOrNick(e.sender)
                    fromUrl = e.sender.avatarUrl
                    toName = getNameOrNick(m)
                    toUrl = m.avatarUrl
                    break
                }
            }
        }
        return suspend {
            service.sendImage(
                e.group, key, BaseConfigFactory.getGifAvatarExtraDataFromUrls(
                    fromUrl, toUrl, e.group.avatarUrl, e.bot.avatarUrl
                ), TextExtraData(
                    fromName, toName, groupName, spanList
                )
            )
        }
    }

    private suspend fun cacheMessageImage(e: GroupMessageEvent) {
        for (singleMessage in e.message) {
            if (singleMessage is Image) {
                val id: Long = e.group.id + e.message.source.ids[0]
                imageCachePool!![id] = singleMessage.queryUrl()
                return
            }
        }
    }

    private suspend fun cacheMessageImage(e: GroupMessagePostSendEvent) {
        for (singleMessage in e.message) {
            if (singleMessage is Image) {
                try {
                    val id: Long = e.target.id + e.receipt!!.source.ids[0]
                    imageCachePool!![id] = singleMessage.queryUrl()
                    return
                } catch (ignore: Exception) {
                }
            }
        }
    }

    private fun isDisabled(group: Group): Boolean {
        return if (disabledGroup != null && disabledGroup!!.isNotEmpty()) {
            disabledGroup!!.contains(group.id)
        } else false
    }

    private fun getNameOrNick(m: Member): String {
        return if (m.nameCard.isEmpty()) m.nick else m.nameCard
    }

    fun isPermission(e: GroupMessageEvent): Boolean {
        return e.permission == MemberPermission.ADMINISTRATOR || e.permission == MemberPermission.OWNER
    }

    private suspend fun sendReplyMessage(e: GroupMessageEvent, text: String) {
        e.group.sendMessage(QuoteReply(e.message).plus(text))
    }

    private fun nudgeEventAreEqual(nudge1: NudgeEvent?, nudge2: NudgeEvent?): Boolean {
        return if (nudge1 == null || nudge2 == null) false else nudge1.bot != nudge2.bot && nudge1.from.id == nudge2.from.id && nudge1.target.id == nudge2.target.id && nudge1.subject.id == nudge2.subject.id
    }

    private fun messageSourceAreEqual(source1: MessageSource?, source2: MessageSource?): Boolean {
        return if (source1 == null || source2 == null) false else source1.targetId == source2.targetId && Arrays.equals(
            source1.ids, source2.ids
        )
    }

        const val VERSION = 4.6f
        private var disabledGroup: MutableList<Long>? = null
        val service = PluginPetService()
        private var previousMessage: MessageSource? = null
        private var previousNudge: NudgeEvent? = null
        private var imageCachePool: LinkedHashMap<Long, String?>? = null
        private var keyAliaSet: kotlin.collections.MutableSet<String>? = null

}