package org.celery.config.main

import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import org.celery.config.AutoSavePluginConfigAutoReloadAble

object MainConfig : AutoSavePluginConfigAutoReloadAble("main-config") {
    @ValueDescription("是否允许私聊调用的默认值")
    val allowFriend: Boolean by value(false)
    @ValueDescription("是否允许陌生人调用的默认值")
    val allowStranger: Boolean by value(false)
    val botOwner by value(0L)
    val superUsers by value(mutableListOf<Long>())
}