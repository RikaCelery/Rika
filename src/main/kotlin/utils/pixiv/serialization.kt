package com.celery.rika.utils.pixiv

import xyz.cssxsh.pixiv.PixivJson
import xyz.cssxsh.pixiv.apps.IllustInfo
import xyz.cssxsh.pixiv.apps.TagInfo
import java.io.File


fun String.deserializeFromJson() = PixivJson.decodeFromString(IllustInfo.serializer(), this)

fun IllustInfo.serializeToJson() = PixivJson.encodeToString(IllustInfo.serializer(), this)

fun File.readIllustInfo(): IllustInfo? {
    if (!exists())
        return null
    return PixivJson.decodeFromString(IllustInfo.serializer(), readText())
}

fun String.deserializeFromStringToInfo(): List<TagInfo> {
    val tmp = mutableListOf<TagInfo>()
    this.split(',').forEach {
        val splits = it.split('=')
        val tmptag = TagInfo(name = splits[0], translatedName = splits[1])
        tmp.add(tmptag)
    }
    return tmp
}

fun List<TagInfo>.serializeTagInfoToString(): String {
    return this.map { it.name.replace(',', '，') + "=" + (it.translatedName?.replace(',', '，') ?: "") }.toString()
        .replace(Regex("[\\[\\]]"), "")
}