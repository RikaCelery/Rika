package org.celery.utils.selenium.styles

object DefaultStyle: CssStyles {
    override fun getStyle() = """
        body{
            font-size: 35px;
            background-color: rgb(0, 0, 0);
        }
        div {
            word-break: break-all;
            color: rgb(255, 255, 255);
            padding: 20px;
            border-style: dashed;
            border-width: 5px;
            border-color: rgb(18, 222, 233);
            border-radius: 30px;
            margin: 20px;
        }
    """.trimIndent()
}