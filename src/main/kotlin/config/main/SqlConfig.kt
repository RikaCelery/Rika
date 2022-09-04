package org.celery.config.main

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object SqlConfig : AutoSavePluginConfig("sqlConfig") {
    @ValueDescription("Hitomi")
    val hitomiName: String by value("adder")
    val hitomiPass: String by value("celery$1806")
    val hitomiUrl: String by value("jdbc:mysql://localhost:3306/HITOMI?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")

    @ValueDescription("Funny")
    val funnyName: String by value("adder")
    val funnyPass: String by value("celery$1806")
    val funnyUrl: String by value("jdbc:mysql://localhost:3306/jokes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Events")
    val eventsName: String by value("adder")
    val eventsPass: String by value("celery$1806")
    val eventsUrl: String by value("jdbc:mysql://localhost:3306/qqmessage2?useSSL=false&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Picture")
    val pictureName: String by value("picture.admin")
    val picturePass: String by value("")
    val pictureUrl: String by value("jdbc:mysql://localhost:3306/groupimagelib?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Pixiv")
    val pixivName: String by value("pixiv.admin")
    val pixivPass: String by value("")
    val pixivUrl: String by value("jdbc:mysql://localhost:3306/pixiv_lib?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("TheWords")
    val theWordsName: String by value("adder")
    val theWordsPass: String by value("celery$1806")
    val theWordsUrl: String by value("jdbc:mysql://localhost:3306/thewords?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")

    @ValueDescription("Reimu")
    val reimuName: String by value("reimu.admain")
    val reimuPass: String by value("reimu\$admain")
    val reimuUrl: String by value("jdbc:mysql://localhost:3306/reimu_lib?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true")
}
