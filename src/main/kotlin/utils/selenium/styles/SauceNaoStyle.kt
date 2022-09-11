package org.celery.utils.selenium.styles

object SauceNaoStyle: CssStyles {
    override fun getStyle() = """    body {
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
        max-width: 100%;
        word-break: break-all;
        display: flex;
        box-sizing: border-box;
    }
    span {
        white-space: normal;
        display: block;
        width: 60%;
        box-sizing: border-box;
    }
    p{
        width: 40%;
        align-items:center;
        text-align: center;
        font-size: 80px;
        display: flex;
        box-sizing: border-box;
        margin: 3px;
    }
    img{
        
        width: 100%;
        height: auto;
        box-sizing: border-box;
        border: 1px solid #ddd;
        border-radius: 14px;
        margin: 1%;
        padding: 10px;  
        box-shadow: 2px 2px 10px 0px rgba(255, 255, 255, 0.5);
    }
    similarity {
        color: #ff934b;
    }


    db-name {
        font-weight: bold;
        color: #55ff60;
    }""".trimIndent()
}