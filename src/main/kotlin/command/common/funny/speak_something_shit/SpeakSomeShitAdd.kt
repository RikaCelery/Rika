package command.common.funny.speak_something_shit

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.common.funny.speak_something_shit.SpeakSomeShit
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.interact.getConfirm
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.strings.getSimilarityRatio
import org.celery.utils.toImage

object SpeakSomeShitAdd : Command(
    "添加批话"
) {

    @Command("^添加批话\\s*(.*)", permissions = arrayOf(RequirePermission.ANY, RequirePermission.OPERATOR))
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (eventMatchResult[1].isBlank()) {
            sendMessage(config["empty_content_reply", "？批话呢"])
            return ExecutionResult.LimitCall
        }

        val find = SpeakSomeShit.list.find { getSimilarityRatio(it, eventMatchResult[1]) > 0.85 }
        if (find != null) {
            if (config["send_image_simiarity_words", true])
                sendMessage(SharedSelenium.render(find).toImage(group))
            if (!getConfirm(config["add_found_simiarity_words", "我艹,发现了一个相似度高达85%的批话,你真的要添加吗"]))
                return ExecutionResult.Ignored("user cancel")
        }
        getOrCreateDataFile("批话.txt", "说批话").appendText(
            eventMatchResult[1]+"\n${SpeakSomeShit.config["seprator", "##########"]}\n"
        )
        sendMessage(config["add_success", "说,都可以说\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75"])
        return ExecutionResult.Success
    }

}


