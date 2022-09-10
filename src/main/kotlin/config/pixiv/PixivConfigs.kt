package config.pixiv

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object PixivConfigs : AutoSavePluginConfig("PixivConfig") {

    @ValueDescription("启用R18的群")
    val enbaleR18: MutableList<Long> by value()
    val ignoreTag: MutableList<String> by value()

    enum class ShowDetail {
        NONE,
        SIMPLE,
        BRIEF,
        FULL,
    }

    val showDetail: MutableMap<Long, ShowDetail> by value()
    val showDetailInGet: MutableMap<Long, ShowDetail> by value()
    val showPrepare: MutableMap<Long, Boolean> by value()

    @ValueDescription("pid自动解析")
    val autoDetect: MutableMap<Long, Boolean> by value()
}