package org.celery.command.common.ero.impl

import command.common.ero.SetuLib
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType
import org.celery.utils.sql.PixivSql
import org.celery.utils.sql.PixivSql.illusts
import org.celery.utils.time.TimeConsts
import org.ktorm.database.asIterable
import org.ktorm.entity.mapColumnsNotNull
import java.util.concurrent.atomic.AtomicBoolean

object SetuPixivLazyLib : SetuLib {
    override val alias: List<String>
        get() = listOf("pixiv")
    override val logger = MiraiLogger.Factory.create(this::class)
    val lock = AtomicBoolean(false)
    val content: MutableList<Setu>
        get() {
            if (lock.get())
                throw Exception("updating database")
            return cache
        }
    var cache: MutableList<Setu> = mutableListOf()
    override val type = SetuType.Pixiv
    override val libName: String
        get() = "pixiv"
    fun getSetuOrNull(sender: Contact, subject: Contact,tag:String): Setu? {

        var setu: Setu? = getMatched(tag)?.find { it.canSand(sender, subject) }
        var trys = 0
        while (setu != null && setu.canSand(sender, subject).not()) {
            setu = getMatched(tag)?.find { it.canSand(sender, subject) }
            if (trys++ >= 25) {
                logger.warning { "在25次尝试中没有找到合适的涩图." }
                break
            }
        }
        return setu
    }
    override fun getRandomOrNull(): Setu? {
        return content.randomOrNull()
    }

    //防sql注入  true = safe
    fun sql_inj(str:String):Boolean {
        val inj_str = "drop|'|and|exec|insert|select|delete|update|" +
                "count|*|%|chr|mid|master|truncate|char|declare|;|or|-|+|,"
        inj_str.split("|").forEach { sqlKeyWord->
            str.split(' ').forEach {
                if (sqlKeyWord==it) {
                    logger.error("sql注入检测到:$sqlKeyWord  match $it  in: $str")
                    return false
                }
            }
        }
        return true//safe
    }
    fun getMatched(tag:String): List<SetuPixivLazy>? {
        if (!sql_inj(tag)){
            throw IllegalArgumentException("检测到sql关键字")
        }
        val time = System.currentTimeMillis()
        return PixivSql.database?.useConnection {//and not illust.shortTag like '%素材%'
            val statement =
                it.prepareStatement("select pid from pixiv_lib.illust where illust.shortTag like '%$tag%' and illust.total_view > 1000 and invalid_artwork = false and visible = true and (deleted = false or deleted IS NULL)  order by rand() limit 15".trimIndent())

            statement.executeQuery().asIterable().map { SetuPixivLazy(it.getLong("pid")) }
        }?.also {
            println("time:${System.currentTimeMillis()-time}ms $tag: "+it.joinToString {it.pid.toString()})
        }
    }

    init{
        kotlin.concurrent.timer("pixiv-lib-auto-update", true, 0, TimeConsts.MIN * 30 * 1000L) {
            try {
                lock.set(true)
                reload()
            } finally {
                lock.set(false)
            }
        }
    }

    private fun reload() {
        val list = mutableListOf<Setu>()
        PixivSql.database?.illusts?.mapColumnsNotNull { it.pid }?.forEach {
            list.add(SetuPixivLazy(it))
        }
        cache.clear()
        cache = list
        logger.info { "成功初始化pixiv图库,当前大小: "+ cache.size }
    }
}