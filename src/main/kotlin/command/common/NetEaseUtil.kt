package command.common

import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.codec.binary.Hex
import java.math.BigInteger
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NetEaseUtil {
    private val client = OkHttpClient.Builder().build()
    private const val AES_KEY = "30313032303330343035303630373038"
    private val MOD: BigInteger
        get() {
            val str =
                "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
            val number: BigInteger = str.toBigInteger(16)
            return number
        }
    private val PUBKEY: Int
        get() {
            val str = "010001"
            val number = str.toInt(16)
            return number
        }
    private val NONCE: ByteArray
        get() {
            val str = "30436f4a556d365179773857386a7564"
            val number = Hex.decodeHex(str)
            return number
        }

    private fun aes(value: String, key: ByteArray): String {
        val toByteArray = Hex.decodeHex(AES_KEY)
        val iv = IvParameterSpec(toByteArray)
        val skeySpec = SecretKeySpec(key, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)
        val encrypted: ByteArray = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)

    }

    private fun rsa(data: ByteArray, pubkey: Int, modulus: BigInteger): String {
        val integer = Hex.encodeHexString(data.reversedArray()).toBigInteger(16)
        val rs = integer.pow(pubkey).mod(modulus)
        return rs.toString(16).padStart(256)
    }

    private fun encryptedRequestBody(jsonBody: String): FormBody {
        val secret = createKey(16)
        val params = aes(aes(jsonBody, NONCE), secret)
        val encSecKey = rsa(secret, PUBKEY, MOD)
        return FormBody.Builder().add("params",params).add("encSecKey",encSecKey).build()
    }

    private fun createKey(size: Int): ByteArray {
        val byteArray = ByteArray(size)
        Random(System.currentTimeMillis()).nextBytes(byteArray)
        return byteArray.map {
            if (it<=0) (-it).toByte() else it
        }.toByteArray()
    }

    fun getLink(ids: List<Long>): Map<Long, String> {
        val reqB = encryptedRequestBody("""{"ids": [${ids.joinToString(",")}], "br": 32000}""")
        val req = Request.Builder().url("http://music.163.com/weapi/song/enhance/player/url")
            .post(reqB).addHeader("Referer","http://music.163.com/").build()
        val resPonse = client.newCall(req).execute()
        val element = Json.parseToJsonElement(resPonse.body!!.string())
       return  element.jsonObject["data"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.long to it.jsonObject["url"]!!.jsonPrimitive.content }.toMap()
    }

    fun getLink(id: Long) = getLink(listOf(id))
}