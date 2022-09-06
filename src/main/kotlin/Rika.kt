package org.celery

import command.common.marry_member.MarryMemberCommand
import command.common.marry_member.MarryMemberCommandBeXioaSan
import net.mamoe.mirai.console.command.Command
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.extension.PluginComponentStorage
import org.celery.command.controller.CommandExecuter
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info
import org.celery.command.builtin.AddBlackList
import org.celery.command.builtin.ConsoleFunctionCallControlDisable
import org.celery.command.builtin.ConsoleFunctionCallControlEnable
import org.celery.command.builtin.FunctionCallControl
import org.celery.command.common.grass_cutter.GrassCutterStatCommand
import org.celery.command.common.marry_member.data.MarryMemberData
import org.celery.command.controller.CCommand
import org.celery.command.controller.EventCommand

object Rika : KotlinPlugin(
    JvmPluginDescription(
        id = "org.celery.rika",
        name = "Rika",
        version = "0.1.0",
    ) {
        author("Celery")
    }
) {
    val DEBUG_MODE: Boolean = true
    val allRegisterdCommand:HashSet<CCommand> = hashSetOf()
    override fun PluginComponentStorage.onLoad() {
        MarryMemberData.reload()
    }
    override fun onEnable() {
        //internal
        CommandExecuter.registerTo(GlobalEventChannel)
        //builtin
        ConsoleFunctionCallControlEnable.register()
        ConsoleFunctionCallControlDisable.register()
        FunctionCallControl.reg()
        AddBlackList.reg()
        //common
//        Test().reg()
        MarryMemberCommand.reg()
        MarryMemberCommandBeXioaSan.reg()
        GrassCutterStatCommand.reg()
//        AA.register()
        logger.info { "Rika loaded" }
    }
}

private fun CCommand.reg(){
    when(this){
        is Command -> {
            register(true)
            Rika.allRegisterdCommand.add(this)
        }
        is EventCommand<*> ->{
            CommandExecuter.add(this)
            Rika.allRegisterdCommand.add(this)
        }
    }
}