package org.celery.config.main

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig("main-config") {
    var defaultCoolDown by value(3000L)
    var defaultCountLimit by value(20)
    val botOwner by value(0L)
    val superUsers by value(mutableListOf<Long>())
}