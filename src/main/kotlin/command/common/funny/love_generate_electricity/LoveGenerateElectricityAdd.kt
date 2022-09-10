package org.celery.command.common.love_generate_electricity

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.interact.getConfirm
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.strings.getSimilarityRatio
import org.celery.utils.toImage

object LoveGenerateElectricityAdd : RegexCommand(
    "添加发电", "^添加发电\\s*(.*)".toRegex(), description = "🥵🥵🥵🥵发，使劲发🥵🥵🥵🥵",
) {


    @Command(ExecutePermission.Operator)
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (eventMatchResult[1].isBlank()) {
            sendMessage("？电呢")
            return ExecutionResult.LimitCall
        }
        val find = LoveGenerateElectricity.list.find { getSimilarityRatio(it, eventMatchResult[1]) > 0.85 }
        if (find != null) {
            sendMessage(SharedSelenium.render(find).toImage(group))
            if (!getConfirm("我艹,发现了一个相似度高达85%的发电话,你真的要添加吗"))
                return ExecutionResult.LimitCall
        }
        getOrCreateDataFile("爱发电.txt","爱发电").appendText(
            eventMatchResult[1].lines().filterNot(String::isBlank).joinToString("", "\n\n")
        )
        sendMessage("发，使劲发\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75")
        return ExecutionResult.Success
    }

}