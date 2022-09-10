package org.celery.utils.selenium

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.warning
import org.celery.Rika.seleniums
import org.celery.utils.file.FileTools
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration

open class Selenium(val debug: Boolean = true) {
    private val logger = MiraiLogger.Factory.create(this::class)
    private var used = false
    private val driver by lazy {
        used = true
        val options = ChromeOptions()
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL)
        options.setHeadless(!debug)
        val time = System.currentTimeMillis()
        val driver1 = ChromeDriver(options)
        driver1.manage().window().size = Dimension(1000, 800)
        driver1.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20))
        driver1.manage().timeouts().implicitlyWait(Duration.ofMillis(5000))
        logger.info("初始化Selenium完成, 耗时: ${System.currentTimeMillis() - time}ms")
        driver1
    }
    val actions by lazy {
        Actions(driver)
    }

    @Synchronized
            /**
             * get
             *
             * delay
             *
             * actionAfterGet()
             *
             * scroll
             */
    fun get(
        url: String,
        delay: Long = 1,
        dimension: Dimension? = null,
        actionAfterGet: suspend (Selenium) -> Unit = {},
        waitUntil: (WebDriver) -> Boolean? = { true }
    ): Selenium {
        try {
            var time = System.currentTimeMillis()
            driver.get(url)
            logger.info("--get耗时: ${System.currentTimeMillis() - time}ms 即将等待${delay}ms")
            Thread.sleep(delay)
            time = System.currentTimeMillis()
            runBlocking {
                actionAfterGet(this@Selenium)
            }
            logger.info("actionAfterGet耗时: ${System.currentTimeMillis() - time}ms")
            time = System.currentTimeMillis()
            driver.executeScript("var q=document.documentElement.scrollTop=100000")
            Thread.sleep(20)
            driver.executeScript("var q=document.documentElement.scrollTop=0")
            logger.info("滚动耗时: ${System.currentTimeMillis() - time}ms ")
            time = System.currentTimeMillis()
            WebDriverWait(driver, Duration.ofSeconds(30)).until {
                waitUntil(it)
            }
            logger.info("WebDriver等待耗时: ${System.currentTimeMillis() - time}ms")
        } catch (e: TimeoutException) {
            logger.warning("TimeoutException: ${e.message}")
        }
        return this
    }

    @Synchronized
    fun getPageSource(url: String, delay: Long = 100): String {
        runBlocking {
            get(url, delay)
        }
        return driver.pageSource ?: ""
    }

    @Synchronized
    fun screenShotElementOrAll(element: WebElement? = null, dimension: Dimension? = null): File {
        val file = FileTools.creatTempFile("png")
        var time = System.currentTimeMillis()
        //resize - X
        val width = driver.executeScript(
            "return Math.max(document.body.scrollWidth, document.body.offsetWidth, document.documentElement.clientWidth, document.documentElement.scrollWidth, document.documentElement.offsetWidth);"
        ).toString().toInt()
        driver.manage().window().size = Dimension(
            dimension?.width?.let { if (it == 0) 1000 else it } ?: (width + 30),
            1
        )
        //resize - Y
        val height = driver.executeScript(
            "return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);"
        ).toString().toInt()
        driver.manage().window().size = Dimension(
            dimension?.width?.let { if (it == 0) 1000 else it } ?: (width + 30),
            dimension?.height?.let { if (it == 0) height else it } ?: height
        )

        logger.info("reSize耗时: ${System.currentTimeMillis() - time}ms")
        time = System.currentTimeMillis()
        if (element != null) {
            val js = driver as JavascriptExecutor
            val yS = element.location.y
            val xS = element.location.x
            js.executeScript("""scrollTo($xS,$yS)""")
        }
//        val pos = element.location
//        val width = element.size.getWidth()
//        val height = element.size.getHeight()
//        val thumb = img.getSubimage(pos.getX(), pos.getY(), width, height)
//        if (!thumbfile.exists())
//            ImageIO.write(thumb, "png", thumbfile)
        val screenshot = (element as? TakesScreenshot)?.getScreenshotAs(OutputType.FILE)
            ?: (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        logger.info("截图耗时: ${System.currentTimeMillis() - time}ms")
        FileTools.cutFile(screenshot, file, true)
        return file
    }

    //    private fun newTempFile(ext: String): File {
//        var file = File("temp/${System.currentTimeMillis()}.$ext")
//        file.parentFile.apply { mkdirs() }
//        var n = 1
//        while (file.exists()){
//            file=File("temp/${System.currentTimeMillis()+n}.$ext")
//            n++
//        }
//        return file
//    }
    @Synchronized
            /**
             * get()
             *
             * `- get
             *
             * `- delay
             *
             * `- actionAfterGet
             *
             * `- scroll
             *
             * preActionBeforeShot()
             *
             * @param delay get后等待时间(ms)
             */
    fun screenShot(
        url: String,
        delay: Long = 1,
        dimension: Dimension? = null,
        waitUntil: (WebDriver) -> Boolean? = { true },
        preActionAfterGet: suspend (Selenium) -> Unit = { },
        preActionBeforeShot: suspend (Selenium) -> Unit = { },
        elementSelector: (Selenium) -> WebElement? = { null }
    ): File {
        runBlocking {
            get(url, delay, dimension, preActionAfterGet, waitUntil)
            val time = System.currentTimeMillis()
            preActionBeforeShot(this@Selenium)
            logger.info("截图前预处理耗时: ${System.currentTimeMillis() - time}ms")
        }
        return screenShotElementOrAll(elementSelector(this), dimension)
    }

    fun clickButton(by: By): Selenium {
        try {
            WebDriverWait(driver, Duration.ofSeconds(20)).until {
                ExpectedConditions.elementToBeClickable(by)
            }
            val element = driver.findElement(by)
            element.click()
        } catch (e: Exception) {
            logger.warning("Element not found by: $by")
        }
        return this
    }

    fun findElement(by: By): WebElement? {
        return try {
            driver.findElement(by)
        } catch (e: Exception) {
            logger.warning("Element not found by: $by")
            null
        }
    }

    fun findElements(by: By): MutableList<WebElement>? {
        return try {
            WebDriverWait(driver, Duration.ofSeconds(40)).until {
                ExpectedConditions.visibilityOfAllElementsLocatedBy(by)
            }
            driver.findElements(by)
        } catch (e: Exception) {
            logger.warning { "Element not found by: $by" }
            null
        }
    }

    init {
        val WORKING_DIR = File("./").canonicalPath
        logger.info("当前工作目录: $WORKING_DIR")
        System.setProperty("webdriver.chrome.driver", "$WORKING_DIR\\chromedriver.exe")
        seleniums.add(this)

    }

    fun quit() {
        logger.info("quit selenium: $this")
        if (used) {
            driver.quit()
        }
        logger.info("quit selenium: $this success")
    }
}