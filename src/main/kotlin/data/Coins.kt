package org.celery.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.User

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
}