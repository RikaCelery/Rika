//
//import com.celery.Plugin
//import com.celery.com.celery.rika.Rika
//import com.celery.functions.newTempFile
//import com.celery.httpUnits.TrustAllManager
//import com.celery.utils.HttpUtils
//import com.celery.com.celery.rika.newTempFile
//import okhttp3.Cache
//import okhttp3.ConnectionPool
//import okhttp3.OkHttpClient
//import java.net.InetSocketAddress
//import java.net.Proxy
//import java.security.SecureRandom
//import java.util.concurrent.TimeUnit
//import javax.net.ssl.HostnameVerifier
//import javax.net.ssl.SSLContext
//import javax.net.ssl.SSLSession
//import javax.net.ssl.SSLSocketFactory
//
//
//class ComHttpClient {
//    var client: OkHttpClient? = null
//    private val cache = Cache(Rika.newTempFile(), 30 * 1024 * 1024)
//
//    val proxy =  Proxy(Proxy.Type.SOCKS, InetSocketAddress(7890))
//    // 忽略Https证书
//    fun init(): ComHttpClient {
//        val connectionPool = ConnectionPool(
//            CONNECTION_POOL_MAX_IDEL,
//            CONNECTION_POOL_KEEP_ALIVE.toLong(), TimeUnit.MINUTES
//        )
//        val trustAllManager = TrustAllManager()
//        client = OkHttpClient().newBuilder()
//            .connectionPool(connectionPool)
//            .proxy(proxy)
//            .connectTimeout(CONNECT_TIMEOUT.toLong(), TimeUnit.SECONDS)
//            .readTimeout(READ_TIMEOUT.toLong(), TimeUnit.SECONDS)
//            .sslSocketFactory(createTrustAllSSLFactory(trustAllManager)!!, trustAllManager)
//            .hostnameVerifier(createTrustAllHostnameVerifier())
//            .cookieJar(HttpUtils.MyCookieJar)
//            .cache(cache)
//            .retryOnConnectionFailure(true).build()
//        return this
//    }
//
//    companion object {
//        fun createTrustAllSSLFactory(trustAllManager: TrustAllManager): SSLSocketFactory? {
//            var ssfFactory: SSLSocketFactory? = null
//            try {
//                val sc: SSLContext = SSLContext.getInstance("TLS")
//                sc.init(null, arrayOf(trustAllManager), SecureRandom())
//                ssfFactory = sc.getSocketFactory()
//            } catch (ignored: Exception) {
//                ignored.printStackTrace()
//            }
//            return ssfFactory
//        }
//
//        //获取HostnameVerifier
//        fun createTrustAllHostnameVerifier(): HostnameVerifier {
//            return object : HostnameVerifier {
//                override fun verify(hostname: String?, session: SSLSession?): Boolean {
//                    return true
//                }
//            }
//        }
//
//        private const val READ_TIMEOUT = 10
//        private const val CONNECT_TIMEOUT = 120
//        private const val CONNECTION_POOL_MAX_IDEL = 60
//        private const val CONNECTION_POOL_KEEP_ALIVE = 20
//    }
//
//    init {
//        init()
//    }
//}