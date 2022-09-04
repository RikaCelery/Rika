package com.celery.rika.utils.pixiv

import io.ktor.http.*
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.config.main.ProxyConfigs
import xyz.cssxsh.pixiv.tool.PixivDownloader
import java.net.InetSocketAddress
import java.net.Proxy

object MyPixivDownloader : PixivDownloader() {
    private val logger = MiraiLogger.Factory.create(this::class, "MyPixivDownloader")
    private val proxyPixiv by lazy {
        PixivDownloader(proxy = Proxy(Proxy.Type.SOCKS,
            InetSocketAddress(ProxyConfigs.pixivPort)))
    }

    override suspend fun download(url: Url): ByteArray {
        return try {
            if (ProxyConfigs.pixivEnable) {
                if (Rika.DEBUG_MODE) logger.debug("proxy download:$url")
                val bytes = proxyPixiv.download(url)
                if (Rika.DEBUG_MODE) logger.debug("proxy download success:$url")
                bytes
            } else {
                if (Rika.DEBUG_MODE) logger.debug("no proxy download:$url")
                val bytes = super.download(url)
                if (Rika.DEBUG_MODE) logger.debug("no proxy download success:$url")
                bytes
            }
        } catch (e: Exception) {
            logger.warning("download error: ${e.message} retry")
            if (ProxyConfigs.pixivEnable)
                proxyPixiv.download(url)
            else
                super.download(url)
        }
    }

}