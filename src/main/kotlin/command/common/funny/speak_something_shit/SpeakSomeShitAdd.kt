package command.common.funny.speak_something_shit

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.common.funny.speak_something_shit.SpeakSomeShit
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.interact.getConfirm
import org.celery.utils.selenium.ShareSelenium
import org.celery.utils.sendMessage
import org.celery.utils.strings.getSimilarityRatio
import org.celery.utils.toImage

object SpeakSomeShitAdd : RegexCommand(
    "添加批话", "^添加批话\\s*(.*)".toRegex(), description = "🥵🥵🥵🥵说,都可以说🥵🥵🥵🥵",
) {
    override var blockSub: Boolean = true

    @Command(ExecutePermission.Operator)
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (eventMatchResult[1].isBlank()) {
            sendMessage("？批话呢")
            return ExecutionResult.LimitCall
        }

        val find = SpeakSomeShit.list.find { getSimilarityRatio(it, eventMatchResult[1]) > 0.85 }
        if (find != null) {
            sendMessage(ShareSelenium.render(find).toImage(group))
            if (!getConfirm("我艹,发现了一个相似度高达85%的批话,你真的要添加吗"))
                return ExecutionResult.LimitCall
        }
        getOrCreateDataFile("批话.txt", "说批话").appendText(
            eventMatchResult[1].lines().filterNot(String::isBlank).joinToString("", "\n##########\n")
        )
        sendMessage("说,都可以说\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75")
        return ExecutionResult.Success
    }

}


