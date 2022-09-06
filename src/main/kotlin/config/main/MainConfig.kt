package org.celery.config.main

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig("main-config") {
    val botOwner by value(0L)
    val superUsers by value(mutableListOf<Long>())
}