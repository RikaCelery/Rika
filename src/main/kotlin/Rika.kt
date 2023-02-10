package org.celery

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info

object Rika : KotlinPlugin(JvmPluginDescription(
    id = "org.celery.rika",
    name = "Rika",
    version = "0.1.0",
) {
    author("Celery")
}) {
    override fun onEnable() {
        Processor.registerTo(GlobalEventChannel)
        PluginManager.add(Test)
        logger.info { "Rika loaded" }
    }
}
