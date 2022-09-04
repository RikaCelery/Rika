package configData

import kotlinx.serialization.Serializable
import xyz.cssxsh.pixiv.PixivConfig

@Serializable
data class Config(
    val debug: Boolean = false,
    val useClient: Boolean,
    val useSql: Boolean,
    val sqlUrl: String,
    val sqlUser: String,
    val sqlPass: String,
    val sqlSchema: String,
    var illustTaskSize: Int = 0,
    var gifTaskSize: Int = 0,
    //uid to token
    val accounts: MutableMap<Long, String>,
    val pixivConfig: PixivConfig,
    var downloadProxy: String = "socks://127.0.0.1:7890",
    var downloadFolder: String? = null,
    var useProxy: Boolean = false
) {
    constructor() : this(
        debug = false,
        useClient = false,
        useSql = false,
        sqlUrl = "jdbc:mysql://localhost:3306/pixiv_lib?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        sqlUser = "ADMIN",
        sqlPass = "PASSWORD",
        sqlSchema = "pixiv_lib",
        illustTaskSize = 64,
        gifTaskSize = 8,
        accounts = mutableMapOf(),
        pixivConfig = PixivConfig(proxy = "socks://127.0.0.1:7890")
    )
}
