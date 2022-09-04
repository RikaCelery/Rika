package org.celery

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import org.celery.command.controller.CommandExecuter
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info
import org.celery.command.temp.AA
import org.celery.command.temp.Test

object Rika : KotlinPlugin(
    JvmPluginDescription(
        id = "org.celery.rika",
        name = "Rika",
        version = "0.1.0",
    ) {
        author("Celery")
    }
) {
    override fun onEnable() {
        CommandExecuter.registerTo(GlobalEventChannel)
        CommandExecuter.add(Test())
        AA.register()
        logger.info { "Rika loaded" }
    }
}