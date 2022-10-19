package org.celery.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.Rika.reload
import java.util.*
import kotlin.concurrent.timer

abstract class AutoSavePluginConfigAutoReloadAble(saveName: String):AutoSavePluginConfig(saveName) {
    private val logger = MiraiLogger.Factory.create(this::class,"Rika-Config:$saveName")
    private val resolveConfigFile = Rika.resolveConfigFile("$saveName.yml")
    private var lastModified = resolveConfigFile.lastModified()
    private val reloader: Timer = timer("auto-reloader-$saveName", true, 0, 1000) {
        if (lastModified != resolveConfigFile.lastModified()) {
            try {
                reload()
            } catch (e: Exception) {
                logger.error(e)
            }
            lastModified = resolveConfigFile.lastModified()
        }
    }
}
