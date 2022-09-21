package org.celery.command.common.ero.impl

import command.common.ero.SetuLib
import command.common.ero.SetuLibManager.saveFolder
import org.celery.command.common.ero.SetuType
import org.celery.command.common.ero.Setu

class SetuLibLocal(
    override val alias: List<String>,
    private val libPath: String,
    override val libName: String=libPath
) :SetuLib {
    val contents: List<Setu>
        get() {
            val libFolder = saveFolder.resolve(libPath)
            if (libFolder.exists().not())
                throw Exception("libPath not exists")
            logger.debug("load lib: $libName from: $libPath got: ${libFolder.listFiles()?.size?:0}")
            val list = libFolder.listFiles()?.map {
                SetuLocal(
                    alias = alias,
                    filePath = it.absolutePath,
                    libName = it.name,
                )
            }
            if (list==null||list.isEmpty())
                throw Exception("lib is empty")
            return list
        }
    override val type: SetuType = SetuType.Local
    override fun getRandomOrNull(): Setu? {
        return contents.randomOrNull()
    }
}