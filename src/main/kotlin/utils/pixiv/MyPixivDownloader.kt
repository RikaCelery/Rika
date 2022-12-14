package org.celery.utils.pixiv

import command.common.ero.ConfigKeys
import command.common.ero.EroConfig
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
        val url = Url(if (EroConfig[ConfigKeys.DOWNLOADER_REVERSE_PROXY_ENABLE,false])
            url.toString().replace("i.pximg.net",EroConfig[ConfigKeys.DOWNLOADER_REVERSE_PROXY_LINK,"i.pixiv.re"])
        else url.toString())
        return try {
//            if (ProxyConfigs.pixivEnable)

            if (EroConfig[ConfigKeys.DOWNLOADER_PROXY_ENABLE,false]){
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
//            logger.warning("download error: ${e.message} retry")
            if (EroConfig[ConfigKeys.DOWNLOADER_PROXY_ENABLE,false]){
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
        }
    }

}