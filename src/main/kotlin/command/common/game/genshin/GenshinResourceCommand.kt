package command.common.game.genshin

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.selenium.Selenium
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import java.io.File

object GenshinResourceCommand : Command(
    "今日素材",
    usage = "今日素材",
) {
    private val selenium by lazy {
        Selenium(false)
    }
    private var imageCache: File? = null

    @Command("^今日素材(.*)")
    suspend fun MessageEvent.on(eventMatchResult: EventMatchResult): ExecutionResult {
        val mode = eventMatchResult[1]
        if (mode == "clear") {
            imageCache = null
            return ExecutionResult.LimitCall
        }
            (imageCache ?: selenium.screenShot(
                "https://genshin.pub/daily",
                dimension = Dimension(550, 1400),
                delay = 2000
            ) {
                it.findElement(By.cssSelector("#next > div > div > div.GSDaily_gs_right_container__u_eYx > div > div.GSContainer_gs_container__2FbUz > div > div > div.GSContainer_inner_border_container__AakE_ > div > div.GSContainer_content_box__1sIXz"))
            }.also { imageCache = it }).inputStream().use {
                it.toExternalResource().use { resource ->
                    subject.sendMessage(At(sender) + subject.uploadImage(resource))
                }
            }
        return ExecutionResult.Success
    }
}