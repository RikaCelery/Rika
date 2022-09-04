package com.celery.rika.utils.selenium

import com.celery.rika.commands.builtin.HelpCommand
import com.celery.rika.utils.file.FileTools
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File

object ShareSelenium : Selenium(false) {
    fun render(html: String): File {
        val tempFile = FileTools.creatTempFile("html")
        tempFile.writeText(html)
        get("file:///${tempFile.absolutePath}")
        val file = screenShotElementOrAll()
        return file
    }
}