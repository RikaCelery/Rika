package org.celery.command.common.funny.speak_something_shit

import events.ExecutionResult
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.data.UserProfile
import net.mamoe.mirai.data.UserProfile.Sex.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.apache.commons.text.StringEscapeUtils
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.file.FileTools
import org.celery.utils.group.GroupTools
import org.celery.utils.selenium.Selenium
import org.celery.utils.sendMessage
import java.util.*

object SpeakSomeShit : RegexCommand(
    "说批话", "^说批话\\s*(.*)$".toRegex(), description = "说点傻逼的"
) {
    val list: List<String>
        get() {
            var sb = StringJoiner("\n")
            val strings = mutableListOf<String>()
            getOrCreateDataFile("批话.txt").readLines().forEach { line ->
                if (line.all { it=='#' }) {
                    strings.add(sb.toString())
                    sb = StringJoiner("\n")
                } else {
                    sb.add(line)
                }
            }
            strings.add(sb.toString())
            return strings.filterNot(String::isBlank)
        }

    private val selenium by lazy { Selenium(false) }

    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (list.isEmpty()) {
            logger.warning("没批话了(批话.txt)")
            sendMessage("没批话了...")
            return ExecutionResult.LimitCall
        }
        val member: String? = eventMatchResult.getResult().groupValues[1].ifBlank { null }
        val target =
            if (member != null && member.startsWith("@")) GroupTools.getUserOrNull(
                subject,
                member
            ) else if (member == null) group.members.random() else null
        val sex = target?.queryProfile()?.sex ?: UserProfile.Sex.FEMALE
        val newValue = target?.nameCardOrNick ?: member!!
        val text = list.random().replace("%name%", newValue)
            .replace("%gender%", sex.let {
                when (it) {
                    MALE -> "他"
                    FEMALE -> "她"
                    UNKNOWN -> "他"
                }
            })
        val X_SIZE = 900
        val perChar = 40
        val allPixels = text.length * perChar
        val y = (allPixels / X_SIZE + 5) * perChar
        val temp = FileTools.creatTempFile("html")
        val replace = StringEscapeUtils.escapeHtml4(text).replace("\n", "<br>")
        temp.writeText(
            """
                <body>
                    <div>$replace</div>
                </body>
                <style>
                    body{
                        font-size: 35px;
                        background-color: rgb(0, 0, 0);
                    }
                    div {
                        color: rgb(255, 255, 255);
                        padding: 20px;
                        border-style: dashed;
                        border-width: 5px;
                        border-color: rgb(18, 222, 233);
                        border-radius: 30px;
                        margin: 20px;
                    }
                </style>
            """.trimIndent()
        )
        val screenShot = selenium.screenShot(
            """file:///$temp""",
            dimension = null
        )
        screenShot.toExternalResource().use {
            group.uploadImage(it)
        }
        sendMessage(screenShot.toExternalResource().use {
            group.uploadImage(it)
        })
        return ExecutionResult.Success
    }

}