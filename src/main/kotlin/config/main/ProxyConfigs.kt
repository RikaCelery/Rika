package org.celery.config.main

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object ProxyConfigs : AutoSavePluginData("proxyData") {
    @ValueDescription("Pixiv代理设置")
    var pixivEnable: Boolean by value(false)
    var pixivPort: Int by value(7890)
    @ValueDescription("HttpClient代理设置")
    var httpClientEnable: Boolean by value(false)
    var httpClientPort: Int by value(7890)
}