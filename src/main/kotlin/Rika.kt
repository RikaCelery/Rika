package org.celery

import command.builtin.coins.MyCoins
import command.common.ero.SetuLibManager
import command.common.funny.baidu_pic_search.BaiduPicSearchBlackWords
import command.common.funny.baidu_pic_search.BaiduPicSearchCommand
import command.common.funny.baidu_pic_search.BaiduPicSearchData
import command.common.funny.love_generate_electricity.LoveGenerateElectricity
import command.common.funny.love_generate_electricity.LoveGenerateElectricityAdd
import command.common.funny.speak_something_shit.SpeakSomeShitAdd
import command.common.game.genshin.GenshinResourceCommand
import command.common.game.genshin.grass_cutter.GrassCutterStatCommand
import command.common.group.exit_notify.MemberExitNotify
import command.common.group.exit_notify.MemberExitNotifyControl
import command.common.group.funny.TalkWithMe
import command.common.group.funny.banme.Banme
import command.common.group.funny.marry_member.MarryMemberCommand
import command.common.group.funny.marry_member.MarryMemberCommandBeXioaSan
import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.manage.AdvanceMute
import command.common.group.manage.KickMember
import command.common.tool.github.Github
import command.common.tool.saucenao.SauceNaoPicSearch
import command.common.tool.what_anime.WhatAnime
import config.pixiv.PixivConfigs
import config.pixiv.config.ConfigData
import data.Limits
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info
import org.celery.Rika.allRegisteredCommand2
import org.celery.command.builtin.*
import org.celery.command.common.ero.commands.RandomSetu
import org.celery.command.common.ero.commands.SpecificSetu
import org.celery.command.common.funny.*
import org.celery.command.common.funny.emoji_mix.AddMix
import org.celery.command.common.funny.emoji_mix.DeleteMix
import org.celery.command.common.funny.emoji_mix.EmojiMix
import org.celery.command.common.funny.speak_something_shit.SpeakSomeShit
import org.celery.command.common.group.funny.WorldCloud
import org.celery.command.common.group.funny.marry_member.MarryMemberCommandDivoce
import org.celery.command.common.group.join_welcom.MemberJoinWelcom
import org.celery.command.common.group.join_welcom.MemberJoinWelcomControl
import org.celery.command.common.group.manage.RequireHeader
import org.celery.command.common.group.manage.RequireHeaderConfirm
import org.celery.command.common.group.mute_notify.BotMuteNotify
import org.celery.command.common.group.mute_notify.MemberMuteNotify
import org.celery.command.common.nbnhhsh.Nbnhhsh
import org.celery.command.common.temp_pick_moon_cake.PickMoonCake
import org.celery.command.controller.CCommand
import org.celery.command.controller.CommandExecutor
import org.celery.command.controller.EventCommand
import org.celery.command.controller.Limitable
import org.celery.command.controller.abs.TestCommand
import org.celery.config.Reloadable
import org.celery.config.main.MainConfig
import org.celery.config.main.ProxyConfigs
import org.celery.config.main.PublicConfig
import org.celery.config.main.SqlConfig
import org.celery.config.main.function.BlackList
import org.celery.config.main.function.WhiteList
import org.celery.data.Coins
import org.celery.listener.QRCodeDector
import org.celery.task.CleanCacheTask
import org.celery.task.CommandDataAutoSave
import org.celery.task.HappyNewYearTask
import org.celery.task.NewTask
import org.celery.utils.selenium.Selenium
import org.celery.utils.task_controller.BotTaskController

object Rika : KotlinPlugin(
    JvmPluginDescription(
        id = "org.celery.rika",
        name = "Rika",
        version = "0.1.0",
    ) {
        author("Celery")
    }
) {
    val seleniums = mutableListOf<Selenium>()
    const val DEBUG_MODE: Boolean = true
    val allRegisteredCommand: HashSet<CCommand> = hashSetOf()
    val allRegisteredCommand2: HashSet<org.celery.command.controller.abs.Command> = hashSetOf()
    override fun PluginComponentStorage.onLoad() {
        logger.info("********************************************************")
        logger.info("reload data")
        MainConfig.reload()
        ProxyConfigs.reload()
        PublicConfig
        SqlConfig.reload()
        ConfigData
        PixivConfigs.reload()
        MarryMemberData.reload()
        BaiduPicSearchData.reload()
        BlackList.reload()
        WhiteList.reload()
        Limits.reload()
        Coins.reload()
        logger.info("reload data end")
        logger.info("********************************************************")
    }


    override fun onEnable() {
        logger.info { "********************************************************" }
//        Limitable.reload()
        //internal
        MessageSaver.registerTo(GlobalEventChannel)
        CommandExecutor.registerTo(GlobalEventChannel)
        //builtin commands
        PanicCommand.reg()
        HelpCommand.reg()
        ConsoleFunctionCallControlEnable.register()
        ConsoleFunctionCallControlDisable.register()
        FunctionCallControl.reg()
//        EditConfig.reg()
        Ban.reg()
        Unban.reg()
        CallControl.reg()
        MyCoins.reg()
        //common commands
        MarryMemberCommand.reg()
        MarryMemberCommandBeXioaSan.reg()
        MarryMemberCommandDivoce.reg()
        GrassCutterStatCommand.reg()
        Banme.reg()
        WorldCloud.reg()
        KickMember.reg()
        AdvanceMute.reg()
        GenshinResourceCommand.reg()
        EmojiMix.reg()
        AddMix.reg()
        DeleteMix.reg()
        Nbnhhsh.reg()
        BaiduPicSearchCommand.reg()
        BaiduPicSearchBlackWords.reg()
        LoveGenerateElectricity.reg()
        LoveGenerateElectricityAdd.reg()
        SpeakSomeShit.reg()
        SpeakSomeShitAdd.reg()
        IKunTimeTransformer.reg()
        MyLuck.reg()
        BegForCoins.reg()
        SearchGeng.reg()
        Github.reg()
        TestCommand.reg()
        NetEase.reg()
        PickMoonCake.reg()
        SauceNaoPicSearch.reg()
        WhatAnime.reg()
        RequireHeader.reg()
        RequireHeaderConfirm.reg()
        MemberExitNotifyControl.reg()
        MemberExitNotify.reg()
        MemberExitNotifyControl.reg()
        MemberExitNotify.reg()
        MemberJoinWelcomControl.reg()
        MemberJoinWelcom.reg()
        TalkWithMe.reg()
        BaiduSearchSBCommand.reg()
        RandomSetu.reg()
        SpecificSetu.reg()
        MemberMuteNotify.reg()
        BotMuteNotify.reg()
        QRCodeDector().registerTo(GlobalEventChannel)
        // add task
        BotTaskController.add(CleanCacheTask())
        BotTaskController.add(CommandDataAutoSave())
        BotTaskController.add(HappyNewYearTask())
        BotTaskController.add(NewTask())
        // start task
        BotTaskController.registerAll()
        runBlocking {
            SetuLibManager
        }
        //check
        mainCheck()
        allRegisteredCommand.forEach(CCommand::perCheck)
        logger.info { "Rika loaded" }
//        logger.info { "********************************************************" }
//        logger.warning { "******** TEST START ********" }
//
//        logger.warning("////TEST ENABLE/CLOSE START////")
//        logger.info("enable:${TestCommand1.isEnable()}")
//        logger.info("close command:${TestCommand1.close()}")
//        logger.info("close command:${TestCommand1.close()}")
//        logger.info("enable:${TestCommand1.isEnable()}")
//        logger.info("enable command:${TestCommand1.enable()}")
//        logger.info("enable command:${TestCommand1.enable()}")
//        logger.info("enable:${TestCommand1.isEnable()}")
//        logger.warning("////TEST ENABLE/CLOSE START////")
//
//        logger.warning("////TEST BAN START////")
//        logger.info("ban subject 100:${TestCommand1.ban(100, 0)}")
//        logger.info("BanList:${BanList.data.toList().joinToString(",") { it.first + "=" + (it.second as Boolean) }}")
//        logger.info("check ban 100,101:${TestCommand1.canNotUse(100, 101)}")
//        logger.info("check ban 100,102:${TestCommand1.canNotUse(100, 102)}")
//        logger.info("check ban 100,103:${TestCommand1.canNotUse(100, 103)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
//        logger.info("unban subject 100:${TestCommand1.unban(100, 0)}")
//        logger.info("BanList:${BanList.data.toList().joinToString(",") { it.first + "=" + (it.second as Boolean) }}")
//        logger.info("check ban 100,101:${TestCommand1.canNotUse(100, 101)}")
//        logger.info("check ban 100,102:${TestCommand1.canNotUse(100, 102)}")
//        logger.info("check ban 100,103:${TestCommand1.canNotUse(100, 103)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
//
//        logger.info("ban user 200:${TestCommand1.ban(0, 200)}")
//        logger.info("BanList:${BanList.data.toList().joinToString(",") { it.first + "=" + (it.second as Boolean) }}")
//        logger.info("check ban  99,200:${TestCommand1.canNotUse(99, 200)}")
//        logger.info("check ban  99,102:${TestCommand1.canNotUse(99, 102)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
//        logger.info("unban user 200:${TestCommand1.unban(0, 200)}")
//        logger.info("BanList:${BanList.data.toList().joinToString(",") { it.first + "=" + (it.second as Boolean) }}")
//        logger.info("check ban  99,200:${TestCommand1.canNotUse(99, 200)}")
//        logger.info("check ban  99,102:${TestCommand1.canNotUse(99, 102)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
//        logger.info("check ban  99,103:${TestCommand1.canNotUse(99, 103)}")
////        logger.info("unban subject 100:${TestCommand1.ban(100,0)}")
////        logger.info("unban user 200:${TestCommand1.ban(100,0)}")
//        logger.warning("////TEST BAN END////")
//
//        logger.warning("////TEST LIMITATION START////")
//        logger.info("get global limit mode for 100,200:${TestCommand1.getLimitMode(100, 200, true)}")
//        logger.info("get normal limit mode for 100,200:${TestCommand1.getLimitMode(100, 200, false)}")
//
//        logger.warning("////TEST GLOBAL LIMITATION////")
//        logger.info("get limit for 100,200:${TestCommand1.getLimit(100,200, 0)}")
//        logger.info("add count for 100,200:${TestCommand1.addCount(100,200, 0)}")
//        logger.info("get count for 100,200:${TestCommand1.getCount(100,200, 0)}")
//        logger.info("add count for  99,200:${TestCommand1.addCount( 99,200, 0)}")
//        logger.info("get count for  99,200:${TestCommand1.getCount( 99,200, 0)}")
//        logger.info("add count for  98,200:${TestCommand1.addCount( 98,200, 0)}")
//        logger.info("get count for  98,200:${TestCommand1.getCount( 98,200, 0)}")
//        logger.info("add count for 100,201:${TestCommand1.addCount(100,201, 0)}")
//        logger.info("get count for 100,201:${TestCommand1.getCount(100,201, 0)}")
//        logger.info("add count for 100,202:${TestCommand1.addCount(100,202, 0)}")
//        logger.info("get count for 100,202:${TestCommand1.getCount(100,202, 0)}")
//        logger.info("add count for 100,203:${TestCommand1.addCount(100,203, 0)}")
//        logger.info("get count for 100,203:${TestCommand1.getCount(100,203, 0)}")
//
//        logger.warning("////TEST SUBJECT LIMITATION////")
//        logger.info("get limit for 100,200:${TestCommand1.getLimit(100,200, 1)}")
//        logger.info("add count for 100,200:${TestCommand1.addCount(100,200, 1)}")
//        logger.info("get count for 100,200:${TestCommand1.getCount(100,200, 1)}")
//        logger.info("add count for  99,200:${TestCommand1.addCount( 99,200, 1)}")
//        logger.info("get count for  99,200:${TestCommand1.getCount( 99,200, 1)}")
//        logger.info("add count for  98,200:${TestCommand1.addCount( 98,200, 1)}")
//        logger.info("get count for  98,200:${TestCommand1.getCount( 98,200, 1)}")
//        logger.info("add count for 100,201:${TestCommand1.addCount(100,201, 1)}")
//        logger.info("get count for 100,201:${TestCommand1.getCount(100,201, 1)}")
//        logger.info("add count for 100,202:${TestCommand1.addCount(100,202, 1)}")
//        logger.info("get count for 100,202:${TestCommand1.getCount(100,202, 1)}")
//        logger.info("add count for 100,203:${TestCommand1.addCount(100,203, 1)}")
//        logger.info("get count for 100,203:${TestCommand1.getCount(100,203, 1)}")
//
//        logger.warning("////TEST USER IN SUBJECT LIMITATION////")
//        logger.info("get limit for 100,200:${TestCommand1.getLimit(100,200, 2)}")
//        logger.info("add count for 100,200:${TestCommand1.addCount(100,200, 2)}")
//        logger.info("get count for 100,200:${TestCommand1.getCount(100,200, 2)}")
//        logger.info("add count for  99,200:${TestCommand1.addCount( 99,200, 2)}")
//        logger.info("get count for  99,200:${TestCommand1.getCount( 99,200, 2)}")
//        logger.info("add count for  98,200:${TestCommand1.addCount( 98,200, 2)}")
//        logger.info("get count for  98,200:${TestCommand1.getCount( 98,200, 2)}")
//        logger.info("add count for 100,201:${TestCommand1.addCount(100,201, 2)}")
//        logger.info("get count for 100,201:${TestCommand1.getCount(100,201, 2)}")
//        logger.info("add count for 100,202:${TestCommand1.addCount(100,202, 2)}")
//        logger.info("get count for 100,202:${TestCommand1.getCount(100,202, 2)}")
//        logger.info("add count for 100,203:${TestCommand1.addCount(100,203, 2)}")
//        logger.info("get count for 100,203:${TestCommand1.getCount(100,203, 2)}")
//
//        logger.warning("////TEST USER LIMITATION////")
//        logger.info("get limit for 100,200:${TestCommand1.getLimit(100,200, 3)}")
//        logger.info("add count for 100,200:${TestCommand1.addCount(100,200, 3)}")
//        logger.info("get count for 100,200:${TestCommand1.getCount(100,200, 3)}")
//        logger.info("add count for  99,200:${TestCommand1.addCount( 99,200, 3)}")
//        logger.info("get count for  99,200:${TestCommand1.getCount( 99,200, 3)}")
//        logger.info("add count for  98,200:${TestCommand1.addCount( 98,200, 3)}")
//        logger.info("get count for  98,200:${TestCommand1.getCount( 98,200, 3)}")
//        logger.info("add count for 100,201:${TestCommand1.addCount(100,201, 3)}")
//        logger.info("get count for 100,201:${TestCommand1.getCount(100,201, 3)}")
//        logger.info("add count for 100,202:${TestCommand1.addCount(100,202, 3)}")
//        logger.info("get count for 100,202:${TestCommand1.getCount(100,202, 3)}")
//        logger.info("add count for 100,203:${TestCommand1.addCount(100,203, 3)}")
//        logger.info("get count for 100,203:${TestCommand1.getCount(100,203, 3)}")
//
//        logger.warning("////TEST LIMITATION END////")
//
//        logger.warning { "********  TEST END  ********" }
//        logger.warning { "********    EXIT    ********" }
        Reloadable.list.forEach { it.save() }
//        exitProcess(0)
    }

    private fun mainCheck() {
        if (MainConfig.botOwner == 0L) logger.warning("未配置bot主人，请将MainConfig中botOwner设置为自己的QQ号")
        if (SqlConfig.hitomiName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的hitomi数据库")
        if (SqlConfig.funnyName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的funny数据库")
        if (SqlConfig.eventsName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的events数据库")
        if (SqlConfig.pictureName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的picture数据库")
        if (SqlConfig.pixivName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的pixiv数据库")
        if (SqlConfig.theWordsName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的theWords数据库")
        if (SqlConfig.reimuName == "name_init") logger.warning("未配置SQL,请修改sqlConfig中的reimu数据库")
        if (ProxyConfigs.httpClientEnable) logger.warning("您开启了网络代理,如果您不清楚这是什么，请关闭")
        if (ProxyConfigs.pixivEnable) logger.warning("您开启了pixiv下载代理,这可能会消耗大量流量")
    }

    override fun onDisable() {
        seleniums.forEach(Selenium::quit)
        super.onDisable()
    }
}

private fun CCommand.reg() {
    when (this) {
        is Command -> {
            register(true)
            Rika.allRegisteredCommand.add(this)
            Limitable.allRegistered.add(this)
        }
        is EventCommand<*> -> {
            CommandExecutor.add(this)
            Rika.allRegisteredCommand.add(this)
            Limitable.allRegistered.add(this)
        }
    }
}
private fun org.celery.command.controller.abs.Command.reg() {
    CommandExecutor.add(this)
    allRegisteredCommand2.add(this)
}