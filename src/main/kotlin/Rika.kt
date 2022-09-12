package org.celery

import com.celery.rika.commands.`fun`.BaiduPicSearchBlackWords
import com.celery.rika.commands.`fun`.BaiduPicSearchData
import command.common.baidu_pic_search.BaiduPicSearchCommand
import command.common.funny.speak_something_shit.SpeakSomeShitAdd
import command.common.game.genshin.GenshinResourceCommand
import command.common.game.genshin.grass_cutter.GrassCutterStatCommand
import command.common.group.funny.marry_member.MarryMemberCommand
import command.common.group.funny.marry_member.MarryMemberCommandBeXioaSan
import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.manage.AdvanceMute
import command.common.group.manage.KickMember
import command.common.what_anime.WhatAnime
import config.pixiv.PixivConfigs
import config.pixiv.config.ConfigData
import data.Limits
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info
import org.celery.command.builtin.*
import org.celery.command.common.NetEase
import org.celery.command.common.TestCommand
import org.celery.command.common.funny.IKunTimeTransformer
import org.celery.command.common.funny.MyLuck
import org.celery.command.common.funny.SearchGeng
import org.celery.command.common.funny.emoji_mix.AddMix
import org.celery.command.common.funny.emoji_mix.DeleteMix
import org.celery.command.common.funny.emoji_mix.EmojiMix
import org.celery.command.common.funny.speak_something_shit.SpeakSomeShit
import org.celery.command.common.github.Github
import org.celery.command.common.group.`fun`.banme.Banme
import org.celery.command.common.group.funny.WorldCloud
import org.celery.command.common.love_generate_electricity.LoveGenerateElectricity
import org.celery.command.common.love_generate_electricity.LoveGenerateElectricityAdd
import org.celery.command.common.nbnhhsh.Nbnhhsh
import org.celery.command.common.saucenao.SauceNaoPicSearch
import org.celery.command.common.temp_pick_moon_cake.PickMoonCake
import org.celery.command.controller.CCommand
import org.celery.command.controller.CommandExecutor
import org.celery.command.controller.EventCommand
import org.celery.command.controller.Limitable
import org.celery.config.main.MainConfig
import org.celery.config.main.ProxyConfigs
import org.celery.config.main.PublicConfig
import org.celery.config.main.SqlConfig
import org.celery.data.Coins
import org.celery.task.CleanCacheTask
import org.celery.task.CommandDataAutoSave
import org.celery.task.HappyNewYearTask
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
    val allRegisteredCommand:HashSet<CCommand> = hashSetOf()
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
        Limits.reload()
        Coins.reload()
        logger.info("reload data end")
        logger.info("********************************************************")
    }
    override fun onEnable() {
        logger.info { "********************************************************" }
        Limitable.reload()
        //internal
        MessageSaver.registerTo(GlobalEventChannel)
        CommandExecutor.registerTo(GlobalEventChannel)
        //builtin commands
        PanicCommand.reg()
        HelpCommand.reg()
        ConsoleFunctionCallControlEnable.register()
        ConsoleFunctionCallControlDisable.register()
        FunctionCallControl.reg()
        EditConfig.reg()
        AddBlackList.reg()
        CallControl.reg()
        //common commands
        MarryMemberCommand.reg()
        MarryMemberCommandBeXioaSan.reg()
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
        TestCommand.reg()
        MyLuck.reg()
        SearchGeng.reg()
        Github.reg()
        NetEase.reg()
        PickMoonCake.reg()
        SauceNaoPicSearch.reg()
        WhatAnime.reg()
        // add task
        BotTaskController.add(CleanCacheTask())
        BotTaskController.add(CommandDataAutoSave())
        BotTaskController.add(HappyNewYearTask())
        // start task
        BotTaskController.registerAll()
        //check
        mainCheck()
        allRegisteredCommand.forEach(CCommand::perCheck)
        logger.info { "Rika loaded" }

        logger.info { "********************************************************" }
    }

    private fun mainCheck() {
        if (MainConfig.botOwner==0L) logger.warning("未配置bot主人，请将MainConfig中botOwner设置为自己的QQ号")
        if (SqlConfig.hitomiName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的hitomi数据库")
        if (SqlConfig.funnyName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的funny数据库")
        if (SqlConfig.eventsName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的events数据库")
        if (SqlConfig.pictureName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的picture数据库")
        if (SqlConfig.pixivName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的pixiv数据库")
        if (SqlConfig.theWordsName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的theWords数据库")
        if (SqlConfig.reimuName=="name_init") logger.warning("未配置SQL,请修改sqlConfig中的reimu数据库")
        if (ProxyConfigs.httpClientEnable) logger.warning("您开启了网络代理,如果您不清楚这是什么，请关闭")
        if (ProxyConfigs.pixivEnable) logger.warning("您开启了pixiv下载代理,这可能会消耗大量流量")
    }

    override fun onDisable() {
        seleniums.forEach(Selenium::quit)
        Limitable.save()
        super.onDisable()
    }
}

private fun CCommand.reg(){
    when(this){
        is Command -> {
            register(true)
            Rika.allRegisteredCommand.add(this)
        }
        is EventCommand<*> ->{
            CommandExecutor.add(this)
            Rika.allRegisteredCommand.add(this)
        }
    }
}