package org.celery.utils.file

import kotlinx.coroutines.delay
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.utils.commandline.runCommand
import java.io.File
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("unused")
object FileTools {
    private val logger = Rika.logger

    @JvmStatic
    fun copyFile(source: File, target: File, overWrite: Boolean = false) {
        if (overWrite || !target.exists()) {
            Files.copy(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES,
            )
        } else {
            logger.info("File $target already exists")
        }
    }

    @JvmStatic
    fun cutFile(source: File, target: File, overWrite: Boolean = false) {
        copyFile(source, target, overWrite)
        if(!source.delete())
            logger.warning("$source 删除失败")
    }

    /**
     * 返回一个随机文件(需要自行新建)
     */
    @Synchronized
    @JvmStatic
    fun getTempFile(ext: String = ""): File {
        var file = File("${Rika.dataFolderPath}/temp/${System.currentTimeMillis()}.$ext")
        file.parentFile.apply { mkdirs() }
        var n = 1
        while (file.exists()) {
            file = File("temp/${System.currentTimeMillis() + n}.$ext")
            n++
        }
        return file
    }

    /**
     * 新建并返回一个随机文件
     */
    @Synchronized
    @JvmStatic
    fun creatTempFile(ext: String = ""): File {
        var file = File("${Rika.dataFolderPath}/temp/${System.currentTimeMillis()}.$ext")
        file.parentFile.apply { mkdirs() }
        var n = 1
        while (file.exists()) {
            file = File("C:\\Users\\14368\\Desktop\\temp\\temp/${System.currentTimeMillis() + n}.$ext")
            n++
        }
        return file.apply { createNewFile() }
    }

    @JvmStatic
    suspend fun getArchive(
        inputFiles: List<File>, passWord: String? = null, overWrite: Boolean = false
    ) = getArchive(creatTempFile(), inputFiles, passWord, overWrite)

    /**
     * 获取本地文件的大小
     */
    @JvmStatic
    fun getFileContentLength(path: String?): Long {
        val file = File(path)
        return if (file.exists() && file.isFile) file.length() else 0
    }

    @JvmStatic
    suspend fun getArchive(
        outPutFile: File, inputFiles: List<File>, passWord: String? = null, overWrite: Boolean = false
    ): File {
        val logger = MiraiLogger.Factory.create(this::class, "7zTool")
        if (!overWrite && outPutFile.exists()) {
            logger.info("$outPutFile already exists")
            return outPutFile
        }
        val tmpList = Rika.resolveDataFile("temp/${inputFiles.hashCode()}.txt")
        tmpList.writeText(inputFiles.joinToString("\n") { it.absolutePath })
        if (passWord.isNullOrEmpty().not()) {
            val callback = "7z a -o${outPutFile.parentFile} -p$passWord $outPutFile @${tmpList}".runCommand()
            logger.info("Password: '$passWord'")
            if (Rika.DEBUG_MODE) logger.debug(callback.inputStream.bufferedReader().readText())
        } else {
            val callback = "7z a -o${outPutFile.parentFile} $outPutFile @${tmpList}".runCommand()
            logger.info("No Password")
            if (Rika.DEBUG_MODE) logger.debug(callback.inputStream.bufferedReader().readText())
        }
        var n = 0
        while (!outPutFile.exists() && n < 60) {
            delay(1000)
            n++
        }
        tmpList.delete()
        if (!outPutFile.exists()) {
            logger.error(
                "$outPutFile not found passWord: $passWord overWrite: $overWrite files: ${
                    inputFiles.joinToString(
                        "\n"
                    )
                }"
            )
            throw FileNotFoundException("压缩文件不存在")
        }
        return outPutFile
    }

    private val ROUNDING_MODE = RoundingMode.HALF_UP

    /**
     * The number of bytes in a kilobyte.
     */
    private const val ONE_KB: Long = 1024

    /**
     * The number of bytes in a kilobyte.
     *
     * @since 2.4
     */
    val ONE_KB_BI: BigInteger = BigInteger.valueOf(ONE_KB)

    /**
     * The number of bytes in a megabyte.
     */
    private const val ONE_MB = ONE_KB * ONE_KB

    /**
     * The number of bytes in a megabyte.
     *
     * @since 2.4
     */
    val ONE_MB_BI: BigInteger = ONE_KB_BI.multiply(ONE_KB_BI)

    /**
     * The number of bytes in a gigabyte.
     */
    private const val ONE_GB = ONE_KB * ONE_MB

    /**
     * The number of bytes in a gigabyte.
     *
     * @since 2.4
     */
    val ONE_GB_BI: BigInteger = ONE_KB_BI.multiply(ONE_MB_BI)

    /**
     * The number of bytes in a terabyte.
     */
    private const val ONE_TB = ONE_KB * ONE_GB

    /**
     * The number of bytes in a terabyte.
     *
     * @since 2.4
     */
    val ONE_TB_BI: BigInteger = ONE_KB_BI.multiply(ONE_GB_BI)

    /**
     * The number of bytes in a petabyte.
     */
    private const val ONE_PB = ONE_KB * ONE_TB

    /**
     * The number of bytes in a petabyte.
     *
     * @since 2.4
     */
    val ONE_PB_BI: BigInteger = ONE_KB_BI.multiply(ONE_TB_BI)

    /**
     * The number of bytes in an exabyte.
     */
    private const val ONE_EB = ONE_KB * ONE_PB

    /**
     * The number of bytes in an exabyte.
     *
     * @since 2.4
     */
    val ONE_EB_BI: BigInteger = ONE_KB_BI.multiply(ONE_PB_BI)

    /**
     * The number of bytes in a zettabyte.
     */
    private val ONE_ZB: BigInteger = BigInteger.valueOf(ONE_KB).multiply(BigInteger.valueOf(ONE_EB))

    /**
     * The number of bytes in a yottabyte.
     */
    val ONE_YB: BigInteger = ONE_KB_BI.multiply(ONE_ZB)

    /**
     * An empty array of type `File`.
     */
    val EMPTY_FILE_ARRAY = arrayOf<File>()

    internal enum class FileSize(val unit: String, val byteCount: BigInteger) {
        EXABYTE("EB", ONE_EB_BI), PETABYTE("PB", ONE_PB_BI), TERABYTE(
            "TB", ONE_TB_BI
        ),
        GIGABYTE("GB", ONE_GB_BI), MEGABYTE("MB", ONE_MB_BI), KILOBYTE(
            "KB", ONE_KB_BI
        ),
        BYTE("bytes", BigInteger.ONE);

        private fun unit(): String {
            return unit
        }

        private fun byteCount(): BigInteger {
            return byteCount
        }
    }


    private fun byteCountToDisplaySize(fileSize: BigInteger?): String {
        var unit = FileSize.BYTE.unit
        var fileSizeInUnit = BigDecimal.ZERO
        var `val`: String
        for (fs in FileSize.values()) {
            val size_bd = BigDecimal(fileSize)
            fileSizeInUnit = size_bd.divide(BigDecimal(fs.byteCount), 5, ROUNDING_MODE)
            if (fileSizeInUnit >= BigDecimal.ONE) {
                unit = fs.unit
                break
            }
        }

        // always round so that at least 3 numerics are displayed (###, ##.#, #.##)
        `val` =
            if (fileSizeInUnit.divide(BigDecimal.valueOf(100.0), RoundingMode.DOWN) >= BigDecimal.ONE) {
                fileSizeInUnit.setScale(0, ROUNDING_MODE).toString()
            } else if (fileSizeInUnit.divide(BigDecimal.valueOf(10.0), RoundingMode.DOWN) >= BigDecimal.ONE
            ) {
                fileSizeInUnit.setScale(1, ROUNDING_MODE).toString()
            } else {
                fileSizeInUnit.setScale(2, ROUNDING_MODE).toString()
            }

        // trim zeros at the end
        if (`val`.endsWith(".00")) {
            `val` = `val`.substring(0, `val`.length - 3)
        } else if (`val`.endsWith(".0")) {
            `val` = `val`.substring(0, `val`.length - 2)
        }
        return String.format("%s %s", `val`, unit)
    }

    @JvmStatic
    fun byteCountToDisplaySize(size: Long): String {
        return byteCountToDisplaySize(BigInteger.valueOf(size))
    }
}