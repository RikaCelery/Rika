package org.celery.utils.selenium

import org.openqa.selenium.Keys

object PreActions {
    object AfterGet {
        const val afterGet = "afterGet"
    }

    object BeforeShot

    val REIMU_OPEN_DOWNLOAD_INFO: suspend (Selenium) -> Unit = {
        it.actions
            .sendKeys(Keys.UP)
            .sendKeys(Keys.UP)
            .sendKeys(Keys.DOWN)
            .sendKeys(Keys.DOWN)
            .sendKeys(Keys.LEFT)
            .sendKeys(Keys.RIGHT)
            .sendKeys(Keys.LEFT)
            .sendKeys(Keys.RIGHT)
            .sendKeys("b")
            .sendKeys("a")
            .perform()
    }
}
