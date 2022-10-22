package org.celery.command.builtin

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.Rika
import org.celery.config.main.MainConfig
import org.celery.utils.sql.MessageEventSql
import kotlin.coroutines.CoroutineContext

object MessageSaver : SimpleListenerHost() {
    @EventHandler(EventPriority.MONITOR)
    suspend fun GroupMessageEvent.on(): ListeningStatus {
        if(!MainConfig.saveMessage){
            return ListeningStatus.STOPPED
        }
        MessageEventSql.addmessage(this, (System.currentTimeMillis() / 1000).toInt())
        return ListeningStatus.LISTENING
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        Rika.logger.error("exception:${exception.event}", exception)
    }

    init {
        if (MainConfig.saveMessage){
            Rika.logger.info("消息保存已开启,若占用空间过大请将main-config,yml中 saveMessage.enable值修改为false")
        }
    }
}