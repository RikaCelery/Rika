package command.common.group.funny.petpet.plugin

import command.common.group.funny.petpet.share.BaseConfigFactory
import command.common.group.funny.petpet.share.BasePetService
import command.common.group.funny.petpet.share.GifAvatarExtraDataProvider
import command.common.group.funny.petpet.share.TextExtraData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.cast
import java.io.File
import java.io.InputStream
import java.util.*

class PluginPetService : BasePetService() {
    var command = "pet"
    var probability: Short = 30
    var commandHead = ""
    var respondSelfNudge = false
    var respondReply = true
    var cachePoolSize = 10000
    var replyFormat: ReplyFormat = ReplyFormat.IMAGE
    var fuzzy = false
    var strictCommand = true
    var messageSynchronized = false
    var headless = true
    var autoUpdate = true
    var updateIgnore: List<String> = ArrayList()
    var repositoryUrl: String? = "https://dituon.github.io/petpet"
    var disabledKey = ArrayList<String>()
    var randomableList = ArrayList<String>()
    var nudgeCanBeDisabled = true
    var messageCanBeDisabled = false
    var disabledGroups: List<Long>? = null
    fun readConfigByPluginAutoSave() {
        val config = PetPetAutoSaveConfig.content
        //        System.out.println("从AutoSaveConfig中读出：" + ConfigDTOKt.encode(config));
        readPluginConfig(config)
    }

    private fun readPluginConfig(config: PluginConfig) {
        readBaseServiceConfig(config.toBaseServiceConfig())
        command = config.command
        antialias = config.antialias
        probability = config.probability
        commandHead = config.commandHead
        respondSelfNudge = config.respondSelfNudge
        respondReply = config.respondReply
        cachePoolSize = config.cachePoolSize ?: 10000
        replyFormat = config.keyListFormat
        fuzzy = config.fuzzy
        strictCommand = config.strictCommand
        messageSynchronized = config.synchronized
        headless = config.headless
        autoUpdate = config.autoUpdate
        updateIgnore = config.updateIgnore
        repositoryUrl = config.repositoryUrl
        disabledGroups = config.disabledGroups
        super.setGifMaxSize(config.gifMaxSize.cast())
        super.encoder = config.gifEncoder
        when (config.disablePolicy) {
            DisablePolicy.NONE -> {
                nudgeCanBeDisabled = false
                messageCanBeDisabled = false
            }
            DisablePolicy.NUDGE -> {
                nudgeCanBeDisabled = true
                messageCanBeDisabled = false
            }
            DisablePolicy.MESSAGE -> {
                nudgeCanBeDisabled = false
                messageCanBeDisabled = true
            }
            DisablePolicy.FULL -> {
                nudgeCanBeDisabled = true
                messageCanBeDisabled = true
            }
        }
        for (path in config.disabled) {
            disabledKey.add(path.replace("\"", ""))
        }
        println("Petpet 初始化成功，使用 $command 以生成GIF。")
    }

    override fun readData(dir: File) {
        // 1. 所有key加载到dataMap
        super.readData(dir)
        // 2. 其中某些key加入randomableList
        dataMap.forEach { path, keyData ->
            if ((!disabledKey.contains(path) && !disabledKey.contains("Type." + keyData.type)) && java.lang.Boolean.TRUE == super.dataMap.get(
                    path
                )!!.inRandomList
            ) {
                randomableList.add(path)
            }
        }
        System.out.println(
            "Petpet 加载完毕 (共 " + dataMap.size + " 素材，已排除 " + (dataMap.size - randomableList.size) + " )"
        )
    }

    /**
     * 发送随机图片
     */
    suspend fun sendImage(group: Group, from: Member, to: Member) { //发送随机图片
        sendImage(group, from, to, randomableList[Random().nextInt(randomableList.size)])
    }

    /**
     * 有概率发送随机图片
     */
    suspend fun sendImage(group: Group, from: Member, to: Member, random: Boolean) {
        if (!random) {
            sendImage(group, from, to)
            return
        }
        val r = Random().nextInt(99) + 1 //不能为0
        if (r >= probability) return
        sendImage(group, from, to)
    }

    /**
     * 用key发送图片(无otherText)
     */
    @Deprecated("")
    suspend fun sendImage(group: Group, from: Member, to: Member, key: String?) {
        sendImage(group, from, to, key, null)
    }

    /**
     * 用key发送图片，指定otherText
     */
    @Deprecated("")
    suspend fun sendImage(group: Group, from: Member, to: Member, key: String?, otherText: String?) {
        val textExtraData = TextExtraData(
            if (from.nameCard.isEmpty()) from.nick else from.nameCard,
            if (to.nameCard.isEmpty()) to.nick else to.nameCard,
            group.name,
            if (otherText == null || otherText == "") ArrayList() else ArrayList(
                Arrays.asList(
                    *otherText.split("\\s+".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                )
            )
        )
        val gifAvatarExtraDataProvider: GifAvatarExtraDataProvider = BaseConfigFactory.getGifAvatarExtraDataFromUrls(
            from.avatarUrl, to.avatarUrl, group.avatarUrl, group.botAsMember.avatarUrl
        )!!
        sendImage(group, key, gifAvatarExtraDataProvider, textExtraData)
    }

    suspend fun sendImage(
        group: Group,
        key: String?,
        gifAvatarExtraDataProvider: GifAvatarExtraDataProvider?,
        textExtraData: TextExtraData?
    ) {
        val generatedImageAndType: Pair<InputStream?, String>? = generateImage(
            key, gifAvatarExtraDataProvider, textExtraData, null
        )
        try {
            if (generatedImageAndType != null) {
                val resource: ExternalResource = (generatedImageAndType.first!!.toExternalResource())
                val image = group.uploadImage(resource)
                withContext(Dispatchers.IO) {
                    resource.close()
                }
                group.sendMessage(image)
            } else {
                println("生成图片失败")
            }
        } catch (ex: Exception) {
            println("发送图片时出错：" + ex.message)
            ex.printStackTrace()
        }
    }

    val keyAliasListString: String
        get() = keyListString!!
}