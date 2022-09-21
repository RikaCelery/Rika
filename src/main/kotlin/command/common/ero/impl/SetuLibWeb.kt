package org.celery.command.common.ero.impl

import command.common.ero.SetuLib
import org.celery.command.common.ero.Setu
import org.celery.command.common.ero.SetuType

class SetuLibWeb(
    override val alias: List<String>,
    private val apiUrl:String,
    val responseType:String?,
    val picKey:String?,
    override val libName: String,
    val save: Boolean=false,
) :SetuLib {

    override val type: SetuType = SetuType.Local
    override fun toString(): String {
        return "别名:${alias} 名称:${libName} 类型:${type}  ApiUrl:${apiUrl}(${responseType?:"image"}) jsonKey:${picKey} 保存:${save}"
    }
    override fun getRandomOrNull(): Setu {
        return SetuWeb(
            alias = alias,
            libName = libName,
            apiUrl = apiUrl,
            responseType = responseType,
            picKey = picKey,
            save = save
        )
    }
}