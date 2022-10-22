package org.celery.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.User
import org.celery.Rika
import org.celery.Rika.reload
import org.celery.Rika.save
import java.util.*
import kotlin.concurrent.timer

object Coins:AutoSavePluginData("main/coin") {
    private val userCoins: MutableMap<Long,Long> by value()
    operator fun set(userId:Long,value:Number) {
        userCoins[userId] = value.toLong()
    }
    operator fun set(user: User, value:Number) {
        userCoins[user.id] = value.toLong()
    }
    operator fun get(user: User) = userCoins[user.id]?:0
    operator fun get(userId: Long) = userCoins[userId]?:0

    private val resolveConfigFile = Rika.resolveDataFile(saveName+".yml")
    private var lastModified = resolveConfigFile.lastModified()

    private val reloader: Timer = timer("auto-reloader", true, 0, 1000) {
        if (lastModified != resolveConfigFile.lastModified()) {
            try {
                save()
                reload()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lastModified = resolveConfigFile.lastModified()
        }
    }
}