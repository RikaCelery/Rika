package command.common.game.genshin.grass_cutter

import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.RegexCommand
import org.celery.utils.http.HttpUtils
import org.celery.utils.serialization.defaultJson

object GrassCutterStatCommand:RegexCommand(
    "grassCutter",
    "^/私服".toRegex(),
    normalUsage = "/私服",
) {
    init {
        defaultCountLimit = 100
    }
    @Command
    suspend fun MessageEvent.handle(): events.ExecutionResult.Success {
        val stat = defaultJson.decodeFromString(
            GrassCutterStat.serializer(),
            HttpUtils.getStringContent("https://127.0.0.1:444/status/server")
        )
        subject.sendMessage("在线人数:${stat.status.playerCount}\n版本:${stat.status.version}\nip:49.140.121.164:444")
        return events.ExecutionResult.Success
    }
}