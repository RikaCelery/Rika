package org.celery.utils.pixiv.pixivClient

import model.EditThisCookie
import model.toCookie
import config.pixiv.config.ConfigData
import io.ktor.client.network.sockets.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.warning
import xyz.cssxsh.pixiv.PixivAuthClient
import xyz.cssxsh.pixiv.PixivConfig
import xyz.cssxsh.pixiv.PixivJson
import xyz.cssxsh.pixiv.auth.AuthResult
import xyz.cssxsh.pixiv.cookie
import xyz.cssxsh.pixiv.exception.AppApiException
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object PixivClientPool {
    private val timer = Timer()
    private val logger = MiraiLogger.Factory.create(this::class)
    fun client(filter: (AuthClient) -> Boolean = { true }): AuthClient {
        var client: AuthClient?
        if (clients.filter { filter(it.value) }.isEmpty()) {
            logger.debug("没有匹配的客户端")
            throw NoSuchElementException()
        }
        if (clients.filter { it.key !in rateLimits && filter(it.value) }.isEmpty()) {
            logger.debug("匹配的客户端正在被限流，等待中...")
        }
        do {
            client = clients.filter { it.key !in rateLimits && filter(it.value) }.values.randomOrNull()
            Thread.sleep(1000)
        } while (client == null)
        return client
    }

    fun limit(uid: Long) {
        rateLimits.add(uid)
        logger.debug("账号限流$uid,已移出账号池")
        timer.schedule(object : TimerTask() {
            override fun run() {
                rateLimits.remove(uid)
                logger.debug("从账号池恢复限流账号$uid")
            }
            //等待4分钟
        }, 4 * 60 * 1000)
    }

    class TempClient : PixivAuthClient() {
        public override var auth: AuthResult? = null

        override val config: PixivConfig = ConfigData.config.pixivConfig

        override val coroutineContext: CoroutineContext = EmptyCoroutineContext + SupervisorJob()

        override val ignore: suspend (Throwable) -> Boolean get() = { handle(it) }
    }

    private suspend fun auth(block: suspend (PixivAuthClient) -> Unit) {
        val client = TempClient()
        try {
            block.invoke(client)
        } catch (e: Exception) {
            when {
                "跳转到" in e.message.toString() -> {
                    logger.debug("exception when authing, retry...")
                    auth(block)
                    return
                }
                "不正确的请求" in e.message.toString() -> {
                    logger.debug("exception when authing, retry...")
                    auth(block)
                    return
                }
                else -> throw e
            }
        }
        val auth = client.auth
        if (auth != null) {
            ConfigData.config.accounts[auth.user.uid] = auth.refreshToken
            clients[auth.user.uid] = AuthClient(uid = auth.user.uid)
        }
    }

    class AuthClient(uid: Long, config: PixivConfig = ConfigData.config.pixivConfig.copy()) : PixivAuthClient() {

        constructor(uid: Long, token: String) : this(uid, ConfigData.config.pixivConfig.copy(refreshToken = token)) {
            runBlocking {
                refresh()
                checkNotNull(auth)
                logger.info("账户 ${auth!!.user.name}#${auth!!.user.uid} 从RefreshToken登陆成功, Age:${this@AuthClient.ageLimit.name}")
            }
        }

        val uid: Long = uid
        override val config: PixivConfig = config

        public override var auth: AuthResult? = null

        override val coroutineContext: CoroutineContext = EmptyCoroutineContext + SupervisorJob()

        override val ignore: suspend (Throwable) -> Boolean = { handle(it) }
    }

    val clients: MutableMap<Long, AuthClient>
    val rateLimits: MutableList<Long> = mutableListOf()
    private val handle: suspend PixivAuthClient.(Throwable) -> Boolean = { throwable ->
        when (throwable) {
            is SocketTimeoutException,
            is ConnectTimeoutException -> {
                logger.debug("Api 超时, 已忽略: ${throwable.message}")
                true
            }
            is IOException -> {
                logger.debug("Api 错误, 已忽略: ${throwable.message}")
                true
            }
            is AppApiException -> {
                val url = throwable.response.request.url
                val request = throwable.response.request.headers.toMap()
                val response = throwable.response.headers.toMap()
                when {
                    "Please check your Access Token to fix this." in throwable.message -> {
                        logger.warning { "PIXIV API OAuth 错误, 将刷新 Token $url with $request" }
                        try {
                            refresh()
                        } catch (cause: Throwable) {
                            logger.warning { "刷新 Token 失败 $cause" }
                        }
                        true
                    }
                    "Rate Limit" in throwable.message -> {
                        logger.warning { "PIXIV API限流, 将延时: ${60000}ms $url with $response" }
                        delay(60000)
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }

    init {
        File("cookies").mkdirs()
        if (ConfigData.config.useClient) {
            logger.info("pixiv账户池初始化中")
            clients = ConfigData.config.accounts.map {
                it.key to
                        AuthClient(
                            it.key,
                            it.value
                        )
            }.toMap().toMutableMap()



            File("cookies").listFiles()?.forEach { file ->
                runBlocking {
                    logger.info("加载 cookie 从 ${file.absolutePath}")
                    val cookies =
                        PixivJson.decodeFromString<List<EditThisCookie>>(file.readText()).map { it.toCookie() }
                    auth { pixiv ->
                        val auth = pixiv.cookie {
                            cookies
                        }
                        logger.info("账户 ${auth.user.name}#${auth.user.uid} 登陆成功，RefreshToken: ${auth.refreshToken}, Age:${pixiv.ageLimit.name} 已保存至config.json")
                        file.delete()
                    }
                }
            }
            logger.info("成功登录${clients.size}个pixiv账户")
//            clients.forEach { (_, u) ->
//                colorln("${u.auth!!.user.name}#${u.auth!!.user.uid} Age: ${u.ageLimit.name}").cyan()
//            }
        } else {
            clients = mutableMapOf()
            logger.warning("未启用pixiv账号！！")
        }
        if (clients.isEmpty())
            logger.warning("无可用pixiv账号！！！,请在浏览器中登录pixiv账号然后使用editMyCookie导出cookie到程序bin路径下的 cookies 文件夹中")
    }
}