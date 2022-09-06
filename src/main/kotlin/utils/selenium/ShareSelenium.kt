package org.celery.utils.selenium

import org.celery.utils.file.FileTools
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