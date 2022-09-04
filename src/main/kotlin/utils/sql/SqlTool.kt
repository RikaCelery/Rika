package com.celery.rika.utils.sql

import com.celery.rika.utils.pixiv.serializeTagInfoToString
import com.celery.rika.utils.pixiv.serializeToJson
import com.celery.rika.utils.sql.PixivSql.illusts
import org.ktorm.entity.add
import org.ktorm.entity.update
import xyz.cssxsh.pixiv.AgeLimit
import xyz.cssxsh.pixiv.PublicityType
import xyz.cssxsh.pixiv.SanityLevel
import xyz.cssxsh.pixiv.WorkContentType
import xyz.cssxsh.pixiv.apps.IllustInfo
import xyz.cssxsh.pixiv.apps.UserInfo
import java.sql.SQLIntegrityConstraintViolationException
import java.time.OffsetDateTime


fun IllustInfo.insert() = PixivSql.database?.illusts?.add(
    PixivSql.Illust {
        title = this@insert.title
        uid = this@insert.user.id
        userName = this@insert.user.name
        pid = this@insert.pid
        tags = this@insert.tags.serializeTagInfoToString()
        totalViews = this@insert.totalView
        totalBookmarks = this@insert.totalBookmarks
        totalComments = this@insert.totalComments
        sanityLevel = this@insert.sanityLevel.name
        pageCount = this@insert.pageCount
        age = this@insert.age.name
        visible = this@insert.visible
        origin = this@insert.serializeToJson()
    }
)

fun IllustInfo.update() = PixivSql.database?.illusts?.update(
    PixivSql.Illust {
        title = this@update.title
        uid = this@update.user.id
        userName = this@update.user.name
        pid = this@update.pid
        tags = this@update.tags.serializeTagInfoToString()
        totalViews = this@update.totalView
        totalBookmarks = this@update.totalBookmarks
        totalComments = this@update.totalComments
        sanityLevel = this@update.sanityLevel.name
        pageCount = this@update.pageCount
        age = this@update.age.name
        visible = this@update.visible
        origin = this@update.serializeToJson()
    }
)

fun IllustInfo.insertOrUpdate() = try {
    insert()
} catch (e: SQLIntegrityConstraintViolationException) {
    update()
}

private fun blankInfo(id: Long) = IllustInfo(
    "", OffsetDateTime.now(), 0, 0, id, mapOf(),
    isBookmarked = false,
    isMuted = false,
    metaPages = listOf(),
    metaSinglePage = mapOf(),
    pageCount = 0,
    restrict = PublicityType.PRIVATE,
    sanityLevel = SanityLevel.UNCHECKED,
    series = null,
    tags = listOf(),
    title = "",
    tools = listOf(),
    totalBookmarks = 0,
    totalComments = 0,
    totalView = 0,
    type = WorkContentType.ILLUST,
    user = UserInfo("", 0, name = "", profileImageUrls = mapOf()),
    visible = false,
    age = AgeLimit.ALL
)


