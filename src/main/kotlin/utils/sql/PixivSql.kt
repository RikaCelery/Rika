package com.celery.rika.utils.sql

import configData.ConfigData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.mamoe.mirai.utils.MiraiLogger
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.*
import org.ktorm.support.mysql.MySqlDialect
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext

object PixivSql : CoroutineScope {
    private val logger = MiraiLogger.Factory.create(this::class)
    val database: Database?

    interface Illust : Entity<Illust> {
        companion object : Entity.Factory<Illust>()

        var title: String
        var uid: Long
        var userName: String
        var pid: Long
        var tags: String
        var totalViews: Long?
        var totalBookmarks: Long?
        var totalComments: Long?
        var sanityLevel: String
        var pageCount: Int
        var age: String
        var visible: Boolean
        var origin: String
        var invalidArtwork: Boolean
        var deleted: Boolean
    }

    object Illusts : Table<Illust>("illust") {
        val title = text("title").bindTo { it.title }
        val uid = long("uid").bindTo { it.uid }
        val userName = text("user_name").bindTo { it.userName }
        val pid = long("pid").primaryKey().bindTo { it.pid }
        val tags = text("tags").bindTo { it.tags }
        val totalViews = long("total_view").bindTo { it.totalViews }
        val totalBookmarks = long("total_bookmarks").bindTo { it.totalBookmarks }
        val totalComments = long("total_comments").bindTo { it.totalComments }
        val sanityLevel = text("sanity_level").bindTo { it.sanityLevel }
        val pageCount = int("page_count").bindTo { it.pageCount }
        val age = text("age").bindTo { it.age }
        val visible = boolean("visible").bindTo { it.visible }
        val origin = text("origin").bindTo { it.origin }
        val invalidArtwork = boolean("invalid_artwork").bindTo { it.invalidArtwork }
        val deleted = boolean("deleted").bindTo { it.deleted }

    }

    val Database.illusts get() = this.sequenceOf(Illusts)
    override val coroutineContext: CoroutineContext =
        CoroutineScope(Dispatchers.Default).coroutineContext + SupervisorJob()

    init {
        logger.info("initing database")
        var databaseTmp: Database? = null
        var n = 0
        if (ConfigData.config.useSql)
            while (databaseTmp == null) {
                n++
                try {
                    databaseTmp = Database.connect(
                        url = ConfigData.config.sqlUrl,
                        driver = "com.mysql.cj.jdbc.Driver",
                        user = ConfigData.config.sqlUser,
                        password = ConfigData.config.sqlPass,
                        logger = ConsoleLogger(threshold = LogLevel.WARN),
                        dialect = MySqlDialect()
                    )
                } catch (e: SQLException) {
                    logger.warning("数据库连接失败,将在1s后重试(如果您开启了代理请尝试关闭代理),err=$e")
                }
                if (n == 3) {
                    logger.error("数据库连接失败")
                    break
                }
            }
        if (databaseTmp == null) {
            logger.info("数据库连接失败,已关闭数据库功能")
        }
        database = databaseTmp
    }
}