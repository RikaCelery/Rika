package org.celery

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object Processor:ListenerHost, CoroutineScope {
    private val context1 = EmptyCoroutineContext
    override val coroutineContext: CoroutineContext =
        CoroutineExceptionHandler(::handleException) + context1 + SupervisorJob(context1[Job])


    @EventHandler
    suspend fun listenAll(e: Event){
        PluginManager.plugins.sortedBy { it.priority }.forEach {
            (it.call(e))
        }
    }
    private fun handleException(context: CoroutineContext, exception: Throwable) {
        Rika.logger.error(exception)
    }
}
