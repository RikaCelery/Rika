package org.celery.command.common.ero

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.toImage
import java.io.File

/**
 * 代表一个抽象的'涩图'
 *
 * 一个涩图对象可能包含多张图片(Pixiv)
 *
 * 每个涩图对象都应该属于某的图库
 */
interface Setu {
    val alias: List<String>
    val logger: MiraiLogger
        get() = MiraiLogger.Factory.create(this::class)
    val type: SetuType
    val libName: String

    /**
     * 返回涩图包含的全部图片
     */
    suspend fun getFiles(): MutableList<File>

    /**
     * 判断当前聊天对象subject[Contact]下的用户sender[Contact] 是否可以查看该涩图
     */
    fun canSand(sender: Contact, subject: Contact): Boolean

    /**
     * 当 聊天对象subject[Contact]下的用户sender[Contact] 无法查看该涩图时进行回复消息等操作
     */
    suspend fun ifBlock(sender: Contact, subject: Contact)

    /**
     * 在[getFiles]中选一张发送
     *
     * 相比于[getFiles],这个函数不会下载全部图片
     */
    suspend fun sendRandom(subject: Contact): Boolean

    /**
     * 将全部图片转化为图片消息
     */
    suspend fun getImages(sender: Contact, subject: Contact): List<Image>

    /**
     * 获得该涩图的描述，为空则表示没有详细信息
     */
    suspend fun getInfo(sender: Contact, subject: Contact): String?

    /**
     * 自动格式化消息并发送至指定聊天对象
     */
    suspend fun sendTo(messageSource: MessageSource?, subject: Contact): Boolean

    /**
     * for debug
     */
    val biriefInfo
        get() = "Setu:${this.type.name} Lib:${this.libName}"

    /**
     * 上传图片失败时
     */
    suspend fun geiFailedImage(subject: Contact, file: File? = null): Image {
        return withContext(Dispatchers.IO) {
            if (file == null)
                "一张图片上传失败".toImage(subject)
            else
                (Rika.javaClass.classLoader.getResource("UploadFailedSetu.jpg")?.openStream()
                    ?: kotlin.run { SharedSelenium.render("图片${file}上传失败") }.inputStream()).use {
                    it.toImage(subject)
                }
        }
    }
}