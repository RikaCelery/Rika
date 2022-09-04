package com.celery.httpUnits

import java.security.SecureRandom
import java.security.cert.CertificateException

import java.security.cert.X509Certificate
import javax.net.ssl.*


fun createTrustAllSSLFactory(trustAllManager: TrustAllManager): SSLSocketFactory? {
    var ssfFactory: SSLSocketFactory? = null
    try {
        val sc: SSLContext = SSLContext.getInstance("TLS")
        sc.init(null, arrayOf(trustAllManager), SecureRandom())
        ssfFactory = sc.getSocketFactory()
    } catch (ignored: Exception) {
        ignored.printStackTrace()
    }
    return ssfFactory
}

//获取HostnameVerifier
fun createTrustAllHostnameVerifier(): HostnameVerifier {
    return object : HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean {
            return true
        }
    }
}


class TrustAllManager : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls(0)
    }
}