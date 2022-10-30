package command.common.group.funny.petpet.plugin

import command.common.group.funny.petpet.share.BasePetService
import org.celery.Rika
import org.celery.command.common.group.funny.petpet.plugin.Petpet
import org.celery.utils.http.HttpUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.Collectors

object DataUpdater {
    fun autoUpdate() {
        if (!checkUpdate()) updateData()
    }

    fun updateData() {
        println("开始更新PetData")
        val index: UpdateIndex = UpdateIndex.getUpdate(
            getUrlText(Petpet.service.repositoryUrl + "/index.json")!!
        )
        val newPetList: List<String> = index.dataList
        for (pet in newPetList) {
            if (Petpet.service.dataMap.containsKey(pet)
                || Petpet.service.updateIgnore.contains(pet)
            ) continue
            val petDataPath = "/data/xmmt.dituon.petpet/$pet"
            if (!saveAs(petDataPath, "data.json")) {
                println("无法从远程仓库下载PetData: $petDataPath")
                break
            }
            var i: Short = 0
            while (saveAs(petDataPath, "$i.png")) i++
            println("PetData/" + pet + "下载成功 (length:" + i + ')')
        }
        val fontsPath = "/data/xmmt.dituon.petpet/" + BasePetService.FONTS_FOLDER
        var localFonts: List<String?> = ArrayList()
        if (File(fontsPath).exists()) localFonts = Arrays.stream(Objects.requireNonNull(File(fontsPath).listFiles()))
            .map { obj: File -> obj.name }.distinct().collect(Collectors.toList())
        for (font in index.fontList) {
            if (localFonts.contains(font)) continue
            if (!saveAs(fontsPath, font)) {
                println("无法从远程仓库下载PetFont: $fontsPath")
                return
            }
            println("PetFont/" + font + "下载成功")
        }
        println("PetData更新完毕, 正在重新加载")
        Petpet.service.readData(Petpet.dataFolder)
    }

    fun checkUpdate(): Boolean {
        val update: UpdateIndex = UpdateIndex.getUpdate(
            (getUrlText(Petpet.service.repositoryUrl + "/index.json"))!!
        )
        if (Petpet.VERSION !== update.version) println("PetpetPlugin可更新到最新版本: " + update.version + " (当前版本 " + Petpet.VERSION + ")")
        for (pet in update.dataList) {
            if (Petpet.service.dataMap.containsKey(pet)) continue
            println("发现新增PetData")
            return false
        }
        return true
    }

    private fun getUrlText(url: String): String? {
        return try {
            String(HttpUtils.downloader(url))
        } catch (ignored: Exception) {
            Rika.logger.error("无法连接到远程资源: $url",ignored)
            null
        }
    }

    private fun saveAs(path: String, fileName: String): Boolean {
        try {
            URL(Petpet.service.repositoryUrl + path + '/' + fileName).openStream().use { ins ->
                val target = Paths.get(File(".").canonicalPath + path, fileName)
                Files.createDirectories(target.parent)
                Files.copy(ins, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (ignored: IOException) {
            return false
        }
        return true
    }
}