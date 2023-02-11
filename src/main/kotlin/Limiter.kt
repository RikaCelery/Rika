package org.celery

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.events.MessageEvent
import java.util.*

@Serializable
class PluginCallLimitInfo(
    var firstCall: Long = System.currentTimeMillis(),
    var LastCall: Long = System.currentTimeMillis(),
    var CallCount: Int = 0
)

@Serializable
class PluginCallLimiter(
    val key: String,//
//    val interval: Int,
//    val truncate: Int,
//    val times: Int,
//    val timesConfigKey: String,
//    val skipSuperuser: Boolean,
    val mutex: Mutex = Mutex()
)

object Limiter : AutoSavePluginData("limiter") {
    val calls: MutableMap<String, PluginCallLimitInfo> by value(Collections.synchronizedMap(mutableMapOf<String, PluginCallLimitInfo>()))

    suspend fun checkCd(e: Any, key: String, plugin: Plugin, function: Function<*, *, *>): Boolean {
        check(e is net.mamoe.mirai.event.Event) { "cd checker can only check type of net.mamoe.mirai.event.Event" }
        val uid = when (e) {
            is MessageEvent -> e.sender.id.toString()
            else -> {
                Rika.logger.warning("插件只能对message做分用户限流")
                return true
            }
        }


        if (function.callLimits[key] == null) {
            function.callLimits[key] = PluginCallLimiter(key)
        }
        val cl = function.callLimits[key]!!
        cl.mutex.withLock {
            val infoLast = calls[uid+key] ?: return true
            if (System.currentTimeMillis() - infoLast.LastCall < 3200) {
                Rika.logger.warning("%s: 用户%s超过频率限制".format(key,uid))
                return false
            }
            return true
        }
    }

    suspend fun addCall(e: Any, key: String, plugin: Plugin, function: Function<*, *, *>){
        val uid = when (e) {
            is MessageEvent -> e.sender.id.toString()
            else -> {
                Rika.logger.warning("插件只能对message做分用户限流")
                return
            }
        }
        val cl = function.callLimits[key]!!
        cl.mutex.withLock {
            calls[uid+key]?.apply {
                LastCall = System.currentTimeMillis()
                CallCount++
            }?: kotlin.run{ calls[uid+key] = PluginCallLimitInfo() }
            return
        }
    }

    fun checkCallLimit() {

    }
}
