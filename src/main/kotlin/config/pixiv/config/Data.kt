package config.pixiv.config

import kotlinx.serialization.Serializable
import xyz.cssxsh.pixiv.apps.IllustInfo
import xyz.cssxsh.pixiv.apps.UgoiraMetadata


@Serializable
data class Data(
    val illustList: HashSet<IllustInfo> = hashSetOf(),
    val ugoiraList: MutableMap<Long, UgoiraMetadata> = mutableMapOf(),
)
