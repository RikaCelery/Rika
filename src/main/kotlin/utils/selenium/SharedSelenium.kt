package org.celery.utils.selenium

import org.apache.commons.text.StringEscapeUtils
import org.celery.utils.file.FileTools
import org.celery.utils.selenium.styles.CssStyles
import org.celery.utils.selenium.styles.DefaultStyle
import org.openqa.selenium.Dimension
import java.io.File

object SharedSelenium : Selenium(false) {

    /**
     * 渲染图片至文件，直接渲染不做任何处理
     */
    fun renderRaw(html: String): File {
        val tempFile = FileTools.creatTempFile("html")
        tempFile.writeText(html)
        get("file:///${tempFile.absolutePath}")
        return screenShotElementOrAll()
    }

    /**
     * 渲染图片至文件，拼接 html+css
     */
    fun renderRaw(html: String,css:String?=null): File {
        val tempFile = FileTools.creatTempFile("html")
        if (css!=null)
        tempFile.writeText(html+css)
        else
        tempFile.writeText(html)
        get("file:///${tempFile.absolutePath}")
        return screenShotElementOrAll()
    }
    /**
     * 使用默认样式渲染一段文字,会进行html转义
     */
    fun render(content: String,dimension: Dimension?=null): File {
        return render("<div>"+StringEscapeUtils.escapeHtml4(content).replace("\n","<br>")+"</div>",DefaultStyle,dimension)
    }

    /**
     *
     *渲染图片至文件，不转义html
     */
    fun render(body: String, style: CssStyles,dimension: Dimension?=null): File {
        val temp = FileTools.creatTempFile("html")
        temp.writeText(
            """
                <body>
                    $body
                </body>
                <style>
                    ${style.getStyle()}
                </style>
            """.trimIndent()
        )
        return screenShot(
            """file:///$temp""",
            dimension = dimension?:Dimension(1800,0)
        )
    }
    /**
     * ```html
    <body>
    $body
    </body>
    <link href="${css.absolutePath}" rel="stylesheet" type="text/css" />
     *
     *渲染图片至文件
     */
    fun render(body: String, css: File): File {
        val temp = FileTools.creatTempFile("html")
        temp.writeText(
            """
                <body>
                    $body
                </body>
                <link href="${css.absolutePath}" rel="stylesheet" type="text/css" />
            """.trimIndent()
        )
        return screenShot(
            """file:///$temp""",
            dimension = Dimension(1800,0)
        )
    }
}