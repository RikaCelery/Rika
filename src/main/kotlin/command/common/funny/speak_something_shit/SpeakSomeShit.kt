package org.celery.command.common.funny.speak_something_shit

import events.ExecutionResult
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.data.UserProfile
import net.mamoe.mirai.data.UserProfile.Sex.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.debug
import org.apache.commons.text.StringEscapeUtils
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.GroupTools
import org.celery.utils.file.FileTools
import org.celery.utils.selenium.Selenium
import org.celery.utils.sendMessage
import org.openqa.selenium.Dimension
import java.util.*

object SpeakSomeShit : Command(
    "说批话", 5, "娱乐","说点傻逼的", "", ""
) {
    private var lastModify = getOrCreateDataFile("批话.txt").lastModified()

    private var caches: List<String> = getTexts()

    private fun getTexts(): List<String> {
        var sb = StringJoiner("\n")
        val strings = mutableListOf<String>()
        getOrCreateDataFile("批话.txt").readLines().forEach { line ->
            if (line == config["seprator", "##########"]) {
                strings.add(sb.toString())
                sb = StringJoiner("\n")
            } else {
                sb.add(line)
            }
        }
        strings.add(sb.toString())
        return strings.filterNot(String::isBlank)
    }

    val list: List<String>
        get() {
            if (getOrCreateDataFile("批话.txt").lastModified() != lastModify) {
                lastModify = getOrCreateDataFile("批话.txt").lastModified()
                caches = getTexts()
            }
            return caches
        }

    private val selenium by lazy { Selenium(false) }

    @Command("^说(?:(\\d*)[条个句])?(.*)批话\\s*(.*)$")
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (list.isEmpty()) {
            logger.warning("没批话了(批话.txt)")
            sendMessage("没批话了...")
            return ExecutionResult.LimitCall
        }
//        logger.debug(eventMatchResult.getResult().groupValues.toString())
        val count = eventMatchResult[1].toIntOrNull()?:1
        if (count >= 4 || count <= 0) {
            sendMessage("傻逼")
            return ExecutionResult.Success
        }
        val want = eventMatchResult[2].let { if (it.lastOrNull() == '的') it.dropLast(1) else it }
        val rawText = if (want.isNotBlank()) list.filter { it.contains(want) }.shuffled().take(count).ifEmpty { null }
            ?: kotlin.run {
                logger.debug { "want:$want, but not found, return random text" }
                list.shuffled().take(count)
            } else list.shuffled().take(count)
        val member: String? = eventMatchResult[3].ifBlank { null }
        val target =
            if (member != null && member.startsWith("@")) GroupTools.getUserOrNull(
                subject,
                member
            ) else if (member == null) group.members.random() else null
        val sex = target?.queryProfile()?.sex ?: UserProfile.Sex.FEMALE
        val newValue = target?.nameCardOrNick ?: member!!
        val content = rawText.map {
            it.replace("%name%", newValue)
                .replace("%gender%", sex.let {
                    when (it) {
                        MALE -> "他"
                        FEMALE -> "她"
                        UNKNOWN -> "他"
                    }
                })
        }
        val temp = FileTools.creatTempFile("html")
        temp.writeText(
            """
                <body>
                    ${
                content.joinToString("\n") {
                    "<div>" + StringEscapeUtils.escapeHtml4(it).replace("\n", "<br>") + "</div>"
                }
            }
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
            dimension = Dimension(1200, 0)
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