package org.celery.command.common.ero.impl

import command.common.ero.SetuLibManager.saveFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.Rika
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType
import org.celery.utils.file.createParentFolder
import org.celery.utils.file.md5
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.toImage
import java.io.File
import java.net.URLDecoder

/**
// * @param alias 别名,用于匹配用户输入
 * @param apiUrl API接口地址
 * @param libName 涩图库名字,用于程序内部查询
 *
 * */
class SetuWeb(
    override val alias: List<String>,
    override val libName: String,
    val apiUrl: String,
    val responseType: String? = null,
    val picKey: String? = null,
    val save: Boolean = true,
) : Setu {
    override val type: SetuType = SetuType.Web

    override suspend fun getFiles(): MutableList<File> {
        if (responseType != "json") {
            val response = HttpUtils.getResponse(apiUrl)
            val stream = response.body!!.byteStream()
            val imageByte = stream.readBytes()
            val md5 = (imageByte).md5
            var filePath = "PictureLib/PictureLib_WEB/$libName/$md5.jpg"
            when (libName) {
                //IMGPAI
                "cos-imgapi" -> {
                    val name = withContext(Dispatchers.IO) {
                        URLDecoder.decode(response.request.url.pathSegments[1], "utf-8")
                    }
                    val id = response.request.url.pathSegments[2]
                    filePath = "PictureLib/PictureLib_WEB/$libName/$name/$id"
                }
            }
            if (!save) {
                filePath = "temp/${filePath.substringAfterLast('/')}"
            }
            val file = (saveFolder).resolve(filePath).createParentFolder()
            if (!file.exists()) {
                val fileParent = file.parentFile
                if (!fileParent.exists()) fileParent.mkdirs()
                file.writeBytes(imageByte)
                logger.debug("保存图片 $filePath")
            }
            response.close()
            withContext(Dispatchers.IO) {
                stream.close()
            }
            return mutableListOf(file)
        } else {
            logger.debug("获取图库json对象")
            val json = try {
                Json.parseToJsonElement(HttpUtils.getStringContent(apiUrl))
            } catch (e: Exception) {
                logger.error("图库${libName}获取json对象失败")
                logger.error(e.stackTraceToString())
                return mutableListOf()
            }
            logger.debug("获取图库json对象成功,获取图片中")
            val url = try { Regex(
                "https?://([a-zA-Z0-9~!@#$%^()\\_\\-=+\\/?.:;',]*)?",
                RegexOption.MULTILINE
            ).find(json.jsonObject[picKey]!!.toString())!!.value
            } catch (e: Exception) {
                logger.error("图库${libName}获取图片失败,json=$json")
                logger.error(e.stackTraceToString())
                return mutableListOf()
            }
            val response = HttpUtils.getResponse(url)
            val stream = response.body!!.byteStream()
            val imageByte = stream.readBytes()
            val md5 = (imageByte.md5)
            var filePath = "PictureLib/PictureLib_WEB/$libName/$md5.jpg"
            when (libName) {
                //IMGPAI
                "cos-imgapi" -> {
                    val name = withContext(Dispatchers.IO) {
                        URLDecoder.decode(response.request.url.pathSegments[1], "utf-8")
                    }
                    val id = response.request.url.pathSegments[2]
                    filePath = "PictureLib/PictureLib_WEB/$libName/$name/$id"
                }
            }

            if (!save) {
                filePath = "temp/${filePath.substringAfterLast('/')}"
            }
            val file = (saveFolder).resolve(filePath).createParentFolder()
            if (!file.exists()) {
                val fileParent = file.parentFile
                if (!fileParent.exists()) fileParent.mkdirs()
                file.writeBytes(imageByte)
                logger.debug("保存图片 $filePath")
            }
            response.close()
            withContext(Dispatchers.IO) {
                stream.close()
            }
            return mutableListOf(file)
        }
    }

    override fun canSand(sender: Contact, subject: Contact): Boolean = true

    override suspend fun ifBlock(sender: Contact, subject: Contact) {
        subject.sendMessage(PlainText("你不能浏览这张图片"))
    }

    override suspend fun sendTo(messageSource: MessageSource?, subject: Contact): Boolean {
        val messageBuilder = MessageChainBuilder()
        messageSource?.let { messageBuilder.add(QuoteReply(it)) }
        getFiles().ifEmpty { null }?.forEach {
            val resource = it.toExternalResource()
            try {
                val image = subject.uploadImage(resource)
                messageBuilder.append(image)
            } catch (e: Exception) {
                logger.error("上传图片${it}失败${e.stackTraceToString()}")
                return false
            } finally {
                resource.close()
            }
        }?.let {
            subject.sendMessage(messageBuilder.build())
            return true
        }
        return false
    }

    override suspend fun sendRandom(subject: Contact): Boolean {
        return sendTo(null, subject)
    }

    override suspend fun getInfo(sender: Contact, subject: Contact): String? {
        return null
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetuWeb

        if (libName != other.libName) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = libName.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

//    override fun toString(): String {
//        return "SetuWebApi(alias='$alias', apiUrl='$apiUrl', libName='$libName', responseType=$responseType, picKey=$picKey, save=$save, type=$type)"
//    }
}