package org.celery.command.builtin

import kotlinx.serialization.builtins.serializer
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.Rika
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.CCommand
import org.celery.command.controller.CommandBasicUsage
import org.celery.utils.sql.MessageEventSql
import kotlin.coroutines.CoroutineContext

object MessageSaver : SimpleListenerHost(),CCommand {
    override val commandId: String = "消息保存"
    @EventHandler(EventPriority.MONITOR)
    suspend fun GroupMessageEvent.on() {
        getConfig(Boolean.serializer(),"enable")
        MessageEventSql.addmessage(this, (System.currentTimeMillis() / 1000).toInt())
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        Rika.logger.error("exception:${exception.event}", exception)
    }

    override var showTip: Boolean = false
    override var defaultBlockRunModeMode: BlockRunMode = BlockRunMode.Subject

    override fun getUsages(): List<CommandBasicUsage> = listOf()

    init {
        if (getConfig(Boolean.serializer(),"enable",true)){
            Rika.logger.info("消息保存已开启,若占用空间过大请将pluginConfigs.json中 消息保存.enable值修改为false")
        }
    }
}