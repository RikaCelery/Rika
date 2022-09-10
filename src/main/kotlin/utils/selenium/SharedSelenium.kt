package org.celery.utils.selenium

import org.apache.commons.text.StringEscapeUtils
import org.celery.utils.file.FileTools
import java.io.File

object SharedSelenium : Selenium(false) {
    fun renderRaw(html: String): File {
        val tempFile = FileTools.creatTempFile("html")
        tempFile.writeText(html)
        get("file:///${tempFile.absolutePath}")
        return screenShotElementOrAll()
    }

    /**
     * 使用默认样式渲染一段文字
     */
    fun render(content: String): File {
        val temp = FileTools.creatTempFile("html")
        temp.writeText(
            """
                <body>
                    <div>${StringEscapeUtils.escapeHtml4(content).replace("\n", "<br>")}</div>
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
        return screenShot(
            """file:///$temp""",
            dimension = null
        )
    }

    /**
     * ```html
    <body>
    $body
    </body>
    <style>
    $style
    </style>
     *
     *渲染图片至文件
     */
    fun render(body: String, style: String): File {
        val temp = FileTools.creatTempFile("html")
        temp.writeText(
            """
                <body>
                    $body
                </body>
                <style>
                    $style
                </style>
            """.trimIndent()
        )
        return screenShot(
            """file:///$temp""",
            dimension = null
        )
    }
}