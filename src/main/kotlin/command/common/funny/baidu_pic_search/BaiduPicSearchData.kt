package command.common.funny.baidu_pic_search

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object BaiduPicSearchData : AutoSavePluginData("baiduPicSearchBlackWords") {
    val regexList: MutableList<String> by value()
}