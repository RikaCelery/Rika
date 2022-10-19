package org.celery.listener

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.utils.WeChatQRCodeTool
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.Selenium

class QRCodeDector : SimpleListenerHost() {
    val file by lazy { Rika.resolveDataFile("qr.txt") }
    private val logger by lazy { MiraiLogger.Factory.create(this::class) }
    private val selenium by lazy { Selenium(true) }

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun MessageEvent.save() {
        if (subject.id !in listOf(1436898372L))
            return
//        println("handle")
        Regex("https://sharechain.qq.com/.*").findAll(message.getTexts().joinToString("")).forEach {
            try {
                var link = it.value
                println(link)

                if (link.contains("sharechain.qq")) {
                    synchronized(selenium::class.java) {
                        Regex("(http://shp.qpic.cn/collector/[^\"]*)").find(selenium.getPageSource(link))?.groupValues?.get(
                            1)?.let {
                            logger.debug("pic link: " + it)
                            link = it
                        }
                        selenium.get(link)
                        link = WeChatQRCodeTool.decode(HttpUtils.downloader(selenium.driver.currentUrl))!!.single()
                        logger.debug("player: " + link)
//                        selenium.get(link,0)
//                        selenium.driver.cast<ChromiumDriver>()
//                        selenium.driver.cast<ChromiumDriver>().devTools.addListener(Network.responseReceived()) {
//                            logger.debug(it.response.url)
//
//                            if (it.response.url.endsWith(".m3u8") || it.response.url.endsWith(".mp4")) {
//                                file.appendText(" * "+it.response.url + "\n")
//                                selenium.get("chrome://history/")
//                            }
//                        }
//                        selenium.actions.sendKeys(" ")
//                        Thread.sleep(20000)
                        selenium.get("chrome://history/")

                        file.appendText(" * "+link + "\n")
                    }
                }else{
                    val decode = WeChatQRCodeTool.decode(HttpUtils.downloader(link))
                    decode?.forEach {
                        val transFormer: (String) -> String? = {
                            if (it.contains("sharechain.qq"))
                                it
                            else
                                it
                        }
                        file.appendText("- " + it + "\n")
                        it.getSub(5, transFormer = transFormer) { s: String, bytes: ByteArray, i: Int ->
                            file.appendText("  ".repeat(i) + "- " + s + "\n")
                            Rika.resolveDataFile("qr_" + System.currentTimeMillis()).writeBytes(bytes)
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
        message.getImages().forEach {
            try {
                val decode = WeChatQRCodeTool.decode(HttpUtils.downloader(it.queryUrl()))
                decode?.forEach {
                    val transFormer: (String) -> String? = {
                        if (it.contains("sharechain.qq"))
                            it
                        else
                            it
                    }
                    it.getSub(5, transFormer = transFormer) { s: String, bytes: ByteArray, i: Int ->
                        file.appendText("  ".repeat(i) + "- " + s)
                        Rika.resolveDataFile("qr_" + System.currentTimeMillis()).writeBytes(bytes)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun MessageChain.getImages(): List<Image> {
        val images = mutableListOf<Image>()
        for (message in this) {
            if (message is Image) images.add(message)
            if (message is FlashImage) images.add(message.image)
            if (message is ForwardMessage) {
                message.nodeList.forEach {
                    it.messageChain.forEach {
                        if (it is Image) images.add(it)
                        if (it is FlashImage) images.add(it.image)
                        if (it is ForwardMessage) images.addAll(it.toMessageChain().getImages())
                    }
                }
            }

        }
        return images
    }

    private fun MessageChain.getTexts(): List<PlainText> {
        val list = mutableListOf<PlainText>()
        for (message in this) {
            if (message is PlainText) list.add(message)
            if (message is ForwardMessage) {
                message.nodeList.forEach {
                    it.messageChain.forEach {
                        if (it is PlainText) list.add(it)
                        if (it is ForwardMessage) list.addAll(it.toMessageChain().getTexts())
                    }
                }
            }

        }
        return list
    }

    private fun String.getSub(
        maxDepth: Int = 9,
        depth: Int = 1,
        transFormer: (String) -> String? = { it },
        action: (String, ByteArray, depth: Int) -> Unit,
    ): List<String>? {
        if (depth > maxDepth)
            return null
        return try {
            val srcImage = HttpUtils.downloader(this)
            WeChatQRCodeTool.decode(srcImage)?.mapNotNull(transFormer)?.mapNotNull {
                action(it, srcImage, depth)
                it.getSub(maxDepth, depth + 1, transFormer, action)
            }?.flatten()
        } catch (e: Exception) {
            null
        }
    }
}