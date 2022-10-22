package org.celery.command.common

import command.common.NetEaseUtil
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MusicKind
import net.mamoe.mirai.message.data.MusicShare
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.sendMessage
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

object Music : Command("网易云链接解析") {
    @Command("/song\\?id=(\\d+)")
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult) {
        val id = eventMatchResult[1].toLongOrNull() ?: eventMatchResult[2].toLongOrNull() ?: return
        if (!canAccess("https://music.163.com/song?id=$id")) {
            logger.warning("vip歌曲无法解析")
            return
        }
        if (config["send_link", false]) {
            sendMessage(NetEaseUtil.getLink(id)[id]!!)
        } else
            sendMessage( ("https://music.163.com/song?id=$id").toCard(id,sender))

    }

    private fun canAccess(keyContent: String): Boolean {
        try {
            val url = URL(keyContent)
            val urlConnection = url.openConnection()
            val httpURLConnection = urlConnection as HttpURLConnection
            httpURLConnection.requestMethod = "POST"
            httpURLConnection.setRequestProperty("Charset", "UTF-8")
            httpURLConnection.connect()
            val fileLength = httpURLConnection.contentLength
            logger.debug("检测所要下载的文件: URL_path = " + keyContent + ", fileLength = " + fileLength)
            /**
             * [!] 判断是否为付费歌曲。 若一首歌曲是付费歌曲，则网易云的音乐下载链接会404
             */
            if (fileLength == 0) {
                return false
            }
        } catch (e: Exception) {
            // do nothing!
        }
        return true
    }

    private fun getDownloadMusicURL(music_ID: Long): String {
        return ("http://music.163.com/song/media/outer/url?id=" + music_ID + ".mp3")
    }

    private fun String.toCard(id: Long,sender:Member): MusicShare {
        val html = Jsoup.parse(URL(this),5000)
        logger.debug(html.title())
        val title = html.title().substringBefore(" - ")
        val summary = html.title().split(" - ").getOrElse(1) {
            title
        }
        val jumpUrl = "https://music.163.com/song?id=$id"
        val pictureUrl = html.getElementsByAttributeValue("property", "og:image").singleOrNull()?.attr("content")?:sender.avatarUrl
        val musicUrl = NetEaseUtil.getLink(id)[id]!!
        return MusicShare(MusicKind.NeteaseCloudMusic, title, summary, jumpUrl, pictureUrl, musicUrl)
    }
}
