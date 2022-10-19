package org.celery.config.main.function

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import org.celery.Rika
import org.celery.Rika.reload
import java.util.*
import kotlin.concurrent.timer

object BlackList : AutoSavePluginConfig("limits/BlackList") {
    private val logger = Rika.logger
    private val resolveConfigFile = Rika.resolveConfigFile("limits/BlackList.yml")
    private var lastModified = resolveConfigFile.lastModified()
    private val globalSubjects by value(mutableListOf<Long>())
    private val globalUsers by value(mutableListOf<Long>())
    private val userInSubjects by value(mutableMapOf<Long, MutableList<Long>>())

    fun containsSubject(subjectId: Long): Boolean {
        return globalSubjects.contains(subjectId)

    }

    fun containsUser(userId: Long): Boolean {
        return globalUsers.contains(userId)

    }

    fun containsUserInSubject(subjectId: Long, userId: Long): Boolean {
        return userInSubjects[subjectId]?.contains(userId) == true

    }


    fun addSubject(subjectId: Long): Boolean {
        return if (!globalSubjects.contains(subjectId)) {
            globalSubjects.add(subjectId)
        } else false
    }

    fun addUser(userId: Long): Boolean {
        return if (!globalUsers.contains(userId)) {
            globalUsers.add(userId)
        } else false
    }

    fun addUserInSubject(subjectId: Long, userId: Long): Boolean {
        return if (userInSubjects[subjectId] == null || userInSubjects[subjectId]?.contains(userId) == false) {
            userInSubjects[subjectId]?.add(userId) ?: kotlin.run {
                userInSubjects[subjectId] = mutableListOf()
                userInSubjects[subjectId]!!.add(userId)
            }
        } else false
    }


    fun removeSubject(subjectId: Long): Boolean {
        return if (globalSubjects.contains(subjectId)) {
            globalSubjects.remove(subjectId)
        } else false
    }

    fun removeUser(userId: Long): Boolean {
        return if (globalUsers.contains(userId)) {
            globalUsers.remove(userId)
        } else false
    }

    fun removeUserInSubject(subjectId: Long, userId: Long): Boolean {
        return if (userInSubjects[subjectId]?.contains(userId) == true) {
            val remove = userInSubjects[subjectId]!!.remove(userId)
            // remove useless key
            if (userInSubjects[subjectId].isNullOrEmpty()) userInSubjects.remove(subjectId)
            remove
        } else false
    }

    //debug
    fun show() {
        logger.info("**** Global ****")
        globalUsers.forEach {
            logger.info("User: $it")
        }
        globalSubjects.forEach {
            logger.info("Subjects: $it")
        }
        logger.info("**** Subject Specific ****")
        userInSubjects.forEach {
            for (l in it.value) {
                logger.info("S: ${it.key}, U: $l")
            }
        }
    }

    private val reloader: Timer = timer("auto-reloader", true, 0, 1000) {
        if (lastModified != resolveConfigFile.lastModified()) {
            try {
                this@BlackList.reload()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lastModified = resolveConfigFile.lastModified()
        }
    }

    init {
        reload()
    }
}