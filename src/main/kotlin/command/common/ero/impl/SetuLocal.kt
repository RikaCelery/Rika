package org.celery.command.common.ero.impl

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.Rika
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.toImage
import java.io.File

/**
 * 表示一张本地涩图
 * @param alias 别名,用于匹配用户输入
 * @param filePath 图片路径
 * @param libName 涩图库名字,用于程序内部查询
 *
 * */
class SetuLocal(override val alias: List<String>, val filePath:String, override val libName:String): Setu {
    override val type: SetuType = SetuType.Local

    override suspend fun getFiles(): MutableList<File> {
        return mutableListOf(File(filePath))
    }

    override fun canSand(sender: Contact, subject: Contact): Boolean {
        return true
    }

    override suspend fun ifBlock(sender: Contact, subject: Contact) {
        subject.sendMessage("这张图片无法浏览")
    }

    override suspend fun sendTo(messageSource:MessageSource?, subject: Contact): Boolean {
        val messageBuilder = MessageChainBuilder()
        messageSource?.let{ messageBuilder.add(QuoteReply(it)) }
        getFiles().forEach {
            val resource = it.toExternalResource()
            try{
                val image = subject.uploadImage(resource)
                messageBuilder.append(image)
            }catch (e:Exception){
                logger.error("上传图片${it}失败${e.stackTraceToString()}")
                return false
            }finally {
                resource.close()
            }
        }
        subject.sendMessage(messageBuilder.build())
        return true
    }

    override suspend fun sendRandom(subject: Contact): Boolean {
        return sendTo(null,subject)
    }

    override suspend fun getImages(sender: Contact, subject: Contact): List<Image> {
        return buildList {
            getFiles().forEach { file: File ->
                try {
                    val image = file.toImage(subject)
                    add(image)
                } catch (e: Exception) {
                    logger.error("上传图片${file}失败${e.stackTraceToString()}")
                    (Rika.javaClass.classLoader.getResource("UploadFailedSetu.jpg")?.openStream()?: kotlin.run { SharedSelenium.render("图片${file}上传失败") }.inputStream()).use {
                        it.toImage(subject)
                    }
                }
            }
        }
    }

    override suspend fun getInfo(sender: Contact, subject: Contact): String? {
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetuLocal

        if (alias != other.alias) return false
        if (filePath != other.filePath) return false
        if (libName != other.libName) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alias.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + libName.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}