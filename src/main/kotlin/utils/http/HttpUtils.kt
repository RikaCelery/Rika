package org.celery.utils.http

import net.mamoe.mirai.utils.MiraiLogger
import okhttp3.*
import org.apache.commons.codec.binary.Hex
import org.celery.config.main.ProxyConfigs
import org.celery.utils.ProgressBar
import org.celery.utils.file.FileTools
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt


@Suppress("unused")
object HttpUtils {
    private val caches = mutableMapOf<String, String>()
    private val logger = MiraiLogger.Factory.create(this::class)
    fun clear() {
        caches.clear()
    }

    private val cache = Cache(FileTools.creatTempFile(), 10 * 1024 * 1024)

    object MyCookieJar : CookieJar {

        private val cookieStore = HashMap<String, List<Cookie>>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host]
            return cookies ?: listOf()
        }

        fun clearCookie() {
            cookieStore.clear()
        }

        fun haveCookie(url: String): Boolean {
            logger.info("haveCookie: $url, ${cookieStore.containsKey(url)}")
            return cookieStore[url] != null
        }
    }

    private val connectionPool = ConnectionPool(
        60,
        20L, TimeUnit.MINUTES
    )
    private val trustAllManager = TrustAllManager()
    private val clientNoProxy: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(MyCookieJar)
        .connectionPool(connectionPool)
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .sslSocketFactory(createTrustAllSSLFactory(trustAllManager)!!, trustAllManager)
        .hostnameVerifier(createTrustAllHostnameVerifier())
        .cache(cache)
        .retryOnConnectionFailure(true).build()
    private val clientProxy: OkHttpClient
    private var outputStream = ByteArrayOutputStream()
    private fun getClient() = if (ProxyConfigs.httpClientEnable) clientProxy else clientNoProxy

    fun downloadToFile(url: String, ext: String? = null, file: File? = null): File {
        val file1 = file ?: FileTools.creatTempFile()
        val request = Request.Builder()
            .url(url)
            .addHeader("referer", "")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.67"
            )
            .build()
        val latch = CountDownLatch(1)
        val mD5 = MessageDigest.getInstance("md5")
        var fileName: String? = null
        var str: String
        getClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                latch.countDown()
                throw e
            }

            override fun onResponse(call: Call, response: Response) {
                val timer = Timer()
                try {

                    response.body?.byteStream()?.use {
                        file1.outputStream().use { outputStream ->

                            var len: Int
                            val total = response.body!!.contentLength()
                            val sum = AtomicLong(0)
                            val buffer = ByteArray(1024 * 1024)
                            val displaySize = FileTools.byteCountToDisplaySize(total)
                            var delta: Long
                            logger.info("开始向文件${file1}写入数据,共${total} 字节$displaySize")
                            var progress = 1f
                            var lastSum = 0L
                            var counter = 1
                            val progressbar = ProgressBar.build(4)
                            fun printProgress() {
                                progressbar.index = (progress.roundToInt())
                                val get = sum.get()
                                delta = get - lastSum
                                lastSum = get
                                str = ("%6.2f%% %s peak:%s/s\tavg:%s/s\t%s/%s\t%s".format(
                                    progress,
                                    progressbar.stringProgressBar,
                                    delta.times(2).toDisplaySize().split(' ')
                                        .mapIndexed { index, s -> if (index == 0) "%.2f".format(s.toFloat()) else s }
                                        .joinToString(" "),
                                    get.times(2).div(counter).toDisplaySize(),
                                    get.toDisplaySize(),
                                    displaySize,
                                    file1.name
                                ))
                                logger.info(str)
                                counter++
                            }
                            timer.scheduleAtFixedRate(object : TimerTask() {

                                override fun run() {


                                    printProgress()
                                }
                            }, 0, 500)
                            while (it.read(buffer).also { len = it } != -1) {
                                progress = (sum.addAndGet(len.toLong()) * 1.0f / total * 100)
                                outputStream.write(buffer, 0, len)
                                mD5.update(buffer, 0, len)
                            }
                            printProgress()
                            logger.info("文件${file1}写入数据完毕,共${total}字节 $displaySize")
                            fileName = Hex.encodeHex(mD5.digest()).joinToString("")

                        }
                    }
                } finally {
                    timer.cancel()
                    latch.countDown()
                }
            }
        })
        latch.await()
        return if (fileName != null) {

            if (ext != null) {
                Files.move(
                    file1.toPath(),
                    file1.parentFile.resolve(fileName!! + "." + ext).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                file1.parentFile.resolve(fileName!! + "." + ext)
            } else {
                Files.move(
                    file1.toPath(),
                    file1.parentFile.resolve(fileName!!).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                file1.parentFile.resolve(fileName!!)
            }

        } else
            file1
    }

    /**
     * 获取文本返回值(GET)
     */
    @Synchronized
    fun getStringContent(url: String, cache: Boolean = false): String {
        println("have cache: ${caches[url] != null}")
        if (caches[url] != null && cache) {
            println("返回已缓存请求: $url")
            return caches[url]!!
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/plain, */*; q=0.01")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.39"
            )
            .build()
        var response: Response? = null
        var n = 0
        while (response == null) {
            if (n > 5)
                throw TimeoutException("尝试次数过多")
            try {
                response = getClient().newCall(request).execute()
            } catch (e: Exception) {
                println(e)
                Thread.sleep(2000)
            }
            n++
        }
        if (response.headers["Content-Encoding"] == "gzip") {
            logger.info("Content-Encoding: gzip")
            // 解压数据
            val gzipIn = GZIPInputStream(response.body!!.byteStream())
            outputStream.reset() // 重置内存流，以便重新使用
            var byte: Int
            while (gzipIn.read().also { byte = it } != -1) outputStream.write(byte)
            println("gzip解压结果：size = ${outputStream.size()}")
            response.close()
            val content = String(outputStream.toByteArray(), Charsets.UTF_8)
            if (cache && response.code == 200) {
                caches[url] = content
                println("cached $url")
            }
            return content
        }
        val content = response.body?.charStream().use {
            it?.readText()
        }
        if (content != null && cache && response.code == 200) {
            caches[url] = content
            println("cached $url")
        }
        response.close()
        return content ?: ""
    }

    fun getResponse(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/plain, */*; q=0.01")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.39"
            )
            .build()
        return getClient().newCall(request).execute()
    }

    fun downloader(url: String): ByteArray {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 5000
        connection.addRequestProperty("referer", "")
        connection.addRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.67"
        )
        connection.connectTimeout = 10000
        val stream = connection.getInputStream()
        val bytes = stream.readBytes()
        stream.close()
        if (bytes.isEmpty())
            println("empty byte array in: $url")
        return bytes
    }

    fun <R> withClient(block: OkHttpClient.() -> R): R {
        return getClient().block()
    }

    init {
        clientProxy = OkHttpClient.Builder()
            .cookieJar(MyCookieJar)
            .connectionPool(connectionPool)
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(createTrustAllSSLFactory(trustAllManager)!!, trustAllManager)
            .hostnameVerifier(createTrustAllHostnameVerifier())
            .cache(null)
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(ProxyConfigs.httpClientPort)))
            .retryOnConnectionFailure(true).build()
    }
}

private fun Long.toDisplaySize(): String = FileTools.byteCountToDisplaySize(this)
