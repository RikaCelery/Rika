package org.celery.config.main

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig("main-config") {
    @ValueDescription("单指令每天最大调用次数")
    var defultCallLimit by value(5)

    @ValueDescription("单指令cd，单位秒")
    var defultCallCd by value(3L)
    val botOwner by value(0L)
    val superUsers by value(mutableListOf<Long>())
}