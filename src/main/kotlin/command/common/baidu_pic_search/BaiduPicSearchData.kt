package com.celery.rika.commands.`fun`

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object BaiduPicSearchData : AutoSavePluginData("baiduPicSearchBlackWords") {
    val regexList: MutableList<String> by value()
}