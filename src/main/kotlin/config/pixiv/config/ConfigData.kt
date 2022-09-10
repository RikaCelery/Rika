package config.pixiv.config

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import xyz.cssxsh.pixiv.AgeLimit
import xyz.cssxsh.pixiv.PublicityType
import xyz.cssxsh.pixiv.SanityLevel
import xyz.cssxsh.pixiv.WorkContentType
import xyz.cssxsh.pixiv.apps.IllustInfo
import xyz.cssxsh.pixiv.apps.UserInfo
import java.time.OffsetDateTime

@Suppress("unused")
object ConfigData {
    private val ConfigJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
        allowSpecialFloatingPointValues = true
        isLenient = true
    }
    private val mutex = Mutex()
    private val logger = MiraiLogger.Factory.create(ConfigData::class.java,"Rika-ConfigData(Pixiv)")

    var config: Config

    //    private var data: Data
    private fun blankInfo(id: Long) = IllustInfo(
        "",
        OffsetDateTime.now(),
        0,
        0,
        id,
        mapOf(),
        false,
        false,
        listOf(),
        mapOf(),
        0,
        PublicityType.PRIVATE,
        SanityLevel.UNCHECKED,
        null,
        listOf(),
        "",
        listOf(),
        0,
        0,
        0,
        WorkContentType.ILLUST,
        UserInfo("", 0, name = "", profileImageUrls = mapOf()),
        false,
        AgeLimit.ALL
    )


//    suspend fun getUgoira(pid: Long): UgoiraMetadata? {
//        return data.ugoiraList[pid]?:if (config.useClient) PixivClientManager.ugoiraMetadata(pid).ugoira else null
//    }

    private fun Config.save() {
        Rika.resolveDataFile("config.json").writeText(ConfigJson.encodeToString(this))
    }

    //    private fun Data.save() {
//        File("./data.json").writeText(PixivJson.encodeToString(this))
//    }
    fun save() {
        logger.info("saving config_data")
        synchronized(this) {
            val configFile = Rika.resolveDataFile("config.json")
            if (!configFile.exists()) {
                configFile.createNewFile()
                config.save()
            } else {
                config.save()
            }
//            val dataFile = File("./data.json").canonicalFile
//            if (!dataFile.exists()) {
//                dataFile.createNewFile()
//                data.save()
//            } else {
//                data.save()
//            }
        }
        logger.info("config_data saved successfully")
    }



    init {
        logger.info("init config")
        val configFile = Rika.resolveDataFile("config.json")
        if (!configFile.exists()) {
            configFile.createNewFile()
            config = Config()
            config.save()
        } else {
            config = ConfigJson.decodeFromString(Config.serializer(), configFile.readText())
            config.save()
        }
//        val dataFile = File("./data.json").canonicalFile
//        if (!dataFile.exists()) {
//            dataFile.createNewFile()
//            data = Data()
//            data.save()
//        }else{
//            data = PixivJson.decodeFromString(Data.serializer(),dataFile.readText())
//            data.save()
//        }
//        logger.debug("已从本地加载:${data.illustList.size}个插画")
    }
}