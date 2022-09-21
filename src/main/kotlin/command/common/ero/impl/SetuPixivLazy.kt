package org.celery.command.common.ero.impl

import config.pixiv.PixivConfigs
import data.Limits
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType
import org.celery.utils.contact.GroupUploadTool
import org.celery.utils.file.FileTools
import org.celery.utils.pixiv.PixivManager
import org.celery.utils.pixiv.getFiles
import org.celery.utils.pixiv.serializeTagInfoToString
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sql.PixivSql
import org.celery.utils.sql.PixivSql.illusts
import org.celery.utils.toImage
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import xyz.cssxsh.pixiv.PixivJson
import xyz.cssxsh.pixiv.apps.IllustInfo
import java.io.File

class SetuPixivLazy(val pid: Long) : Setu {

    val info by lazy {
        PixivSql.database?.illusts?.find { it.pid eq pid }?.origin?.let {
            PixivJson.decodeFromString(
                IllustInfo.serializer(),
                it
            )
        } ?: runBlocking { PixivManager.illustDetail(pid).illust }
    }
    override val libName: String = "pixiv"
    override val alias: List<String>
        get() = listOf("pixiv")
    override val type: SetuType
        get() = SetuType.Pixiv
    val simpleInfo by lazy {
        "PID: ${info.pid}(${info.age.name})\n" +
                "作者: ${info.user.name}(${info.user.id})"
    }
    val briefInfo by lazy {
        "PID: ${info.pid}\n" +
                "作者: ${info.user.name}\n" +
                "作者UID: ${info.user.id}\n" +
                "标题: ${info.title}\n" +
                "年龄分级: ${info.age.name}\n" +
                "San: ${info.sanityLevel.name}\n"
    }
    val fullInfo by lazy {
        "PID: ${info.pid}\n" +
                "作者: ${info.user.name}\n" +
                "作者UID: ${info.user.id}\n" +
                "标题: ${info.title}\n" +
                "年龄分级: ${info.age.name}\n" +
                "San: ${info.sanityLevel.name}\n" +
                "创作于: ${info.createAt}\n" +
                "标签: ${info.tags.joinToString { it.name + (it.translatedName?.let { s -> "=$s" } ?: "") }}"
    }

    fun getInfoInGet(subject: Contact): String {
        return when (PixivConfigs.showDetailInGet[subject.id]) {
            PixivConfigs.ShowDetail.NONE -> "PID: ${info.pid}"
            PixivConfigs.ShowDetail.SIMPLE -> simpleInfo
            PixivConfigs.ShowDetail.BRIEF -> briefInfo
            PixivConfigs.ShowDetail.FULL -> fullInfo
            else -> simpleInfo
        } + "\n图片数量" + info.pageCount.toString()
    }

    private fun getTextInfo(subject: Contact): String {
        return when (PixivConfigs.showDetail[subject.id]) {
            PixivConfigs.ShowDetail.NONE -> "PID: ${info.pid}"
            PixivConfigs.ShowDetail.SIMPLE -> simpleInfo
            PixivConfigs.ShowDetail.BRIEF -> briefInfo
            PixivConfigs.ShowDetail.FULL -> fullInfo
            else -> simpleInfo
        }
    }

    override suspend fun getFiles(): MutableList<File> = info.getFiles()

    override suspend fun ifBlock(sender: Contact, subject: Contact) {
        logger.warning(
            "${sender.id} in ${subject.id} try to send a ${type.name} with ${info.sanityLevel.name} ${info.age.name} " +
                    "lower than limit ${Limits.getAge(sender.id, subject.id).name} ${
                        Limits.getSan(
                            sender.id,
                            subject.id
                        ).name
                    }"
        )
        val ageLimit = Limits.getAge(sender.id, subject.id).ordinal < info.age.ordinal
        val sanLimit = Limits.getSan(sender.id, subject.id).ordinal < info.sanityLevel.ordinal
        val haveIgnoreTag =
            (PixivConfigs.ignoreTag.any { Regex(it).containsMatchIn(info.tags.serializeTagInfoToString()) })
        val ignoredTag = (PixivConfigs.ignoreTag.mapNotNull {
            Regex(
                it,
                RegexOption.IGNORE_CASE
            ).find(info.tags.serializeTagInfoToString())?.value
        })

        subject.sendMessage(
            "这张图片${
                if (ageLimit) "Age:${info.age.name}超过了你的限制${Limits.getAge(sender.id, subject.id).name}" else "" +
                        if (sanLimit) "SanityLevel:${info.sanityLevel.name}超过了你的限制${
                            Limits.getSan(
                                sender.id,
                                subject.id
                            ).name
                        }" else "" +
                                if (haveIgnoreTag) "包含屏蔽Tag:${ignoredTag}" else ""
            }无法浏览"
        )
    }

    /**
     * 判断一个pixiv涩图是否可以被发送
     * @return true 如果可以发送,否则返回 false
     */
    override fun canSand(sender: Contact, subject: Contact): Boolean {
        if ((info.totalView?:0)<1000)
            return false
        if (PixivConfigs.ignoreTag.any { Regex(it).containsMatchIn(info.tags.serializeTagInfoToString()) })
            return false
        if (Limits.getAge(sender.id, subject.id).ordinal < info.age.ordinal) {
            return false
        }
        if (Limits.getSan(sender.id, subject.id).ordinal < info.sanityLevel.ordinal) {
            return false
        }
        if (Limits.isEro(info.pid, subject.id)) {
            return false
        }
        return true
    }

    companion object {
        /**
         * 判断一个pixiv图片是否可以被发送
         * @return true 如果可以发送,否则返回 false
         */
        fun IllustInfo.canSand(sender: Contact, subject: Contact): Boolean {
            if (Limits.getAge(sender.id, subject.id).ordinal < age.ordinal) {
                return false
            }
            if (Limits.getSan(sender.id, subject.id).ordinal < sanityLevel.ordinal) {
                return false
            }
            return true
        }
    }

    /**
     * 将涩图发送到指定的联系人,数量最大为10张
     */
    override suspend fun sendTo(messageSource: MessageSource?, subject: Contact): Boolean {
        if (PixivConfigs.showPrepare[subject.id] != false && info.pageCount > 1) {
            subject.sendMessage(PlainText("${simpleInfo}\n正在下载图片(${info.pageCount}张)..."))
        }
        val pageCount = info.pageCount
        if (false) {
            val members: MutableList<NormalMember>? = (subject as? Group)?.members?.toMutableList()
            val forwordBuilder = ForwardMessageBuilder(subject)
            forwordBuilder.add(
                members?.random()?.id ?: 0,
                members?.random()?.nameCardOrNick ?: "God",
                PlainText(getTextInfo(subject) + if (info.pageCount > 10) "\n图片数量过多，只显示前10张" else "")
            )
            info.getFiles(10).forEach {
                val resource = it.toExternalResource()
                try {
                    val image = subject.uploadImage(resource)
                    forwordBuilder.add(
                        members?.random()?.id ?: 0,
                        members?.random()?.nameCardOrNick ?: "God",
                        image
                    )
                } catch (e: Exception) {
                    logger.error("发送图片${it}失败${e.stackTraceToString()}")
                }
                resource.close()
            }
            try {
                subject.sendMessage(forwordBuilder.build())
            } catch (e: Exception) {
                logger.error("发送失败${e.stackTraceToString()}")
                return false
            }
        } else {
            val messageBuilder = MessageChainBuilder()
//            //1
//            messageBuilder.add( QuoteReply(messageSource) )
//            // quick fix
//            messageBuilder.add(messageSource?.let { QuoteReply(it) })


            messageSource?.let { messageBuilder.add(QuoteReply(it)) }
            if (pageCount > 5)
                messageBuilder.append(PlainText("已截取5张图片(共${pageCount}张)"))
            messageBuilder.append(PlainText(getTextInfo(subject)))
            info.getFiles(5).forEach {
                val resource = it.toExternalResource()
                try {
                    val image = subject.uploadImage(resource)
                    messageBuilder.append(image)
                } catch (e: Exception) {
                    logger.error("上传图片${it}失败${e.stackTraceToString()}")
                }
                resource.close()
            }
            try {
                subject.sendMessage(messageBuilder.build())
            } catch (e: Exception) {
                logger.error("发送失败${e.stackTraceToString()}")
                return false
            }
        }
        return true
    }

    /**
     * 加密发送所有图片,密码为pid
     */
    suspend fun encryptSendAll(subject: Group): String {
        logger.info("加密发送${info.pid}")
        mutableListOf<Image>()
        val files = mutableListOf<File>()
        files.addAll(getFiles())
        val archeive = FileTools.getArchive(FileTools.creatTempFile("${info.pid}.7z"), files, info.pid.toString())
        GroupUploadTool.uploadFile(subject, archeive, archeive.name, autoDelete = true)
        logger.info("加密发送${info.pid}完成")
        return info.pid.toString()
    }

    /**
     * 向指定联系人随机发送一张图
     */
    override suspend fun sendRandom(subject: Contact): Boolean {
        if (PixivConfigs.showPrepare[subject.id] != false) {
            subject.sendMessage(PlainText("${briefInfo}\n正在下载图片(${info.pageCount}张)..."))
        }
        val messageBuilder = MessageChainBuilder()
        messageBuilder.append(PlainText(getTextInfo(subject)))
        info.getFiles(10).random().let { file ->
            file.toExternalResource().use {
                try {
                    val image = subject.uploadImage(it)
                    messageBuilder.add(image)
                } catch (e: Exception) {
                    logger.error("上传图片${file}失败${e.stackTraceToString()}")
                }
            }
        }
        try {
            subject.sendMessage(messageBuilder.build())
        } catch (e: Exception) {
            logger.error("发送失败${e.stackTraceToString()}")
            return false
        }

        return true
    }

    fun detail(): String {
        return info.title + ", " + info.age.name + ", " + info.sanityLevel.name
    }

    /**
     * 获取消息列表，以图片信息开头，接着是图片
     * @param sender 没有用
     */
    override suspend fun getImages(sender: Contact, subject: Contact): List<Image> {
        return buildList<Image> {
            val files = info.getFiles(21)
            if (files.size >= 20) {
                add(SharedSelenium.render(getTextInfo(subject) + "\n图片数量过多(截取20张)，请使用加密发送全部图片").toImage(subject))
                (files.forEach {
                    try {
                        add(it.toImage(subject))
                    } catch (e: Exception) {
                        logger.error("上传图片${it}失败了...${e.stackTraceToString()}")
                        add(SharedSelenium.render("上传图片${it}失败了...").toImage(subject))
                    }

                })

            } else {
                add(getTextInfo(subject).toImage(subject))
                files.forEach {
                    try {
                        add(it.toImage(subject))
                    } catch (e: Exception) {
                        logger.error("上传图片${it}失败了...${e.stackTraceToString()}")
                        add(
                            geiFailedImage(subject, it)
                        )
                    }
                }
            }
        }
    }

    override suspend fun getInfo(sender: Contact, subject: Contact): String {
        return getInfoInGet(subject)
    }


    /**
     * 获取合并后的消息链
     * 格式为
     * [消息]
     * [图片1]
     * [图片2]
     * ...
     */
    suspend fun getMessage(subject: Contact): Message {
        mutableListOf<Message>()
        val files = info.getFiles(21)

        val msgBuilder = MessageChainBuilder()
        if (files.size >= 21) {
            msgBuilder.append(PlainText(getTextInfo(subject) + "\n图片数量过多(截取20张)，请使用加密发送全部图片"))
            files.take(20).forEach {
                try {
                    it.toExternalResource().use { resource ->
                        val image = subject.uploadImage(resource)
                        msgBuilder.append(image)
                    }
                } catch (e: Exception) {
                    logger.error("上传图片${it}失败了...${e.stackTraceToString()}")
                    msgBuilder.append(PlainText("上传图片失败了..."))
                }
            }
        } else
            files.let {
                msgBuilder.append(PlainText(getTextInfo(subject)))
                it.forEach {
                    try {
                        val image = it.toExternalResource().use {
                            subject.uploadImage(it)
                        }
                        msgBuilder.append(image)
                    } catch (e: Exception) {
                        logger.error("上传图片${it}失败了...${e.stackTraceToString()}")
                        msgBuilder.append(PlainText("上传图片失败了..."))
                    }
                }
            }
        return msgBuilder.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetuPixivLazy

        if (libName != other.libName) return false
        if (type != other.type) return false
        if (fullInfo != other.fullInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = libName.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + fullInfo.hashCode()
        return result
    }
}