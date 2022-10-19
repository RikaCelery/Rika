package org.celery.config.main

import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import org.celery.config.AutoSavePluginConfigAutoReloadAble

object SqlConfig : AutoSavePluginConfigAutoReloadAble("sqlConfig") {
    @ValueDescription("Hitomi数据库")
    val hitomiName: String by value("name_init")
    val hitomiPass: String by value("pass_init")
    val hitomiUrl: String by value("jdbc:mysql://localhost:3306/HITOMI?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")

    @ValueDescription("Funny数据库")
    val funnyName: String by value("name_init")
    val funnyPass: String by value("pass_init")
    val funnyUrl: String by value("jdbc:mysql://localhost:3306/jokes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Events数据库")
    val eventsName: String by value("name_init")
    val eventsPass: String by value("pass_init")
    val eventsUrl: String by value("jdbc:mysql://localhost:3306/qqmessage2?useSSL=false&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Picture数据库")
    val pictureName: String by value("name_init")
    val picturePass: String by value("pass_init")
    val pictureUrl: String by value("jdbc:mysql://localhost:3306/groupimagelib?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Pixiv数据库")
    val pixivName: String by value("name_init")
    val pixivPass: String by value("pass_init")
    val pixivUrl: String by value("jdbc:mysql://localhost:3306/pixiv_lib?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("TheWords数据库")
    val theWordsName: String by value("name_init")
    val theWordsPass: String by value("pass_init")
    val theWordsUrl: String by value("jdbc:mysql://localhost:3306/thewords?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Reimu数据库")
    val reimuName: String by value("name_init")
    val reimuPass: String by value("pass_init")
    val reimuUrl: String by value("jdbc:mysql://localhost:3306/reimu_lib?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")
}
