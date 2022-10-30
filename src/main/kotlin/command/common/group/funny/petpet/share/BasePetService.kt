package command.common.group.funny.petpet.share

import command.common.group.funny.petpet.share.KeyData.Companion.getData
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import xmmt.dituon.share.BaseGifMaker
import xmmt.dituon.share.BaseImageMaker
import xmmt.dituon.share.TextModel
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.*
import java.lang.Boolean
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.function.Function
import java.util.stream.Collectors
import javax.imageio.ImageIO
import kotlin.Array
import kotlin.Deprecated
import kotlin.Exception
import kotlin.Int
import kotlin.Pair
import kotlin.Short
import kotlin.ShortArray
import kotlin.String
import kotlin.Throws
import kotlin.also
import kotlin.arrayOf
import kotlin.assert
import kotlin.shortArrayOf

open class BasePetService {
    protected var antialias = true
    var gifMaxSize: List<Int?>? = null
        private set
    var encoder = Encoder.BUFFERED_STREAM
    protected var dataRoot: File? = null
    var dataMap = HashMap<String, KeyData>()
        protected set
    var aliaMap = HashMap<String, Array<String>>()
        protected set

    open fun readData(dir: File) {
        dataRoot = dir
        val children = dir.list()
        if (children == null) {
            println("无法读取文件，请检查data目录")
            return
        }
        val keyListStringBuilder = StringBuilder()
        for (path in children) {
            if (path == FONTS_FOLDER) {
                // load fonts folder
                registerFontsToAwt(File(dir.absolutePath + File.separator + path))
                continue
            }
            // load templates folder
            // TODO 模板应放在data/templates而不是直接data
            val dataFile = File(dir.absolutePath + File.separator + path + "/data.json")
            try {
                val data = getData(getFileStr(dataFile))
                dataMap[path] = data
                if (Boolean.TRUE == data.hidden) continue
                keyListStringBuilder.append("\n").append(path)
                if (data.alias != null) {
                    keyListStringBuilder.append(" ( ")
                    data.alias.forEach { aliasKey: String ->
                        keyListStringBuilder.append(aliasKey).append(" ")
                        if (aliaMap[aliasKey] == null) {
                            aliaMap[aliasKey] = arrayOf(path)
                            return@forEach
                        }
                        val oldArray = aliaMap[aliasKey]!!
                        val newArray = Arrays.copyOf(oldArray, oldArray.size + 1)
                        newArray[oldArray.size] = path
                        aliaMap[aliasKey] = newArray
                    }
                    keyListStringBuilder.append(")")
                }
            } catch (ex: Exception) {
                println("无法读取 $path/data.json: \n\n$ex")
            }
        }
        keyListString = keyListStringBuilder.toString()
    }

    private fun registerFontsToAwt(fontsFolder: File) {
        if (!fontsFolder.exists() || !fontsFolder.isDirectory) {
            println("无fonts")
            return
        }
        val successNames: MutableList<String> = ArrayList()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        for (fontFile in Objects.requireNonNull(fontsFolder.listFiles())) {
            try {
                val customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile)
                val success = ge.registerFont(customFont)
                if (success) {
                    successNames.add(fontFile.name)
                } else {
                    println("registerFontsToAwt失败: " + fontFile.name)
                }
            } catch (e: Exception) {
                println("registerFontsToAwt异常: $e")
            }
        }
        println("registerFontsToAwt成功: $successNames")
    }

    fun readBaseServiceConfig(config: BaseServiceConfig) {
        antialias = config.antialias
    }

    /**
     * @return InputStream 及其图片格式（值域：["gif", "png"...]）
     */
    @Deprecated("不支持gif, 没有改用多线程, 不建议使用")
    fun generateImage(
        key: String, avatarExtraDataProvider: AvatarExtraDataProvider,
        textExtraData: TextExtraData?,
        additionTextDatas: List<TextData>?
    ): Pair<InputStream?, String>? {
        var key = key
        if (!dataMap.containsKey(key) && !aliaMap.containsKey(key)) {
            println("无效的key: “$key”")
            return null
        }
        val data = if (dataMap.containsKey(key)) dataMap[key] else dataMap[aliaMap[key]!![0]]
        key = dataRoot!!.absolutePath + File.separator +
                (if (dataMap.containsKey(key)) key else aliaMap[key]) + File.separator
        try {
            val textList: ArrayList<TextModel> = ArrayList<TextModel>()
            // add from KeyData
            if (!data!!.text.isEmpty()) {
                for (textElement in data.text) {
                    textList.add(TextModel(textElement, textExtraData))
                }
            }
            // add from params
            if (additionTextDatas != null) {
                for (textElement in additionTextDatas) {
                    textList.add(TextModel(textElement, textExtraData))
                }
            }
            val avatarList: ArrayList<AvatarModel> = ArrayList<AvatarModel>()
            if (!data.avatar.isEmpty()) {
                for (avatarData in data.avatar) {
                    avatarList.add(AvatarModel(avatarData, avatarExtraDataProvider, data.type))
                }
            }
            val delay: Int = if (data.delay != null) data.delay else 65
            if (data.type === Type.GIF) {
                val stickerMap = HashMap<Short, BufferedImage>()
                var imageNum: Short = 0
                for (file in Objects.requireNonNull(File(key).listFiles())) {
                    if (!file.name.endsWith(".png")) continue
                    stickerMap[imageNum] = ImageIO.read(File(key + imageNum++ + ".png"))
                }
                if (data.background != null) { //从配置文件读背景
                    val sticker: BufferedImage = BackgroundModel(data.background, avatarList, textList).getImage()
                    for (i in 0 until avatarList[0].posLength) {
                        stickerMap[i.toShort()] = sticker
                    }
                }
                val inputStream: InputStream = BaseGifMaker.makeGIF(
                    avatarList, textList, stickerMap, antialias, gifMaxSize, encoder, delay
                )!!
                return Pair<InputStream?, String>(inputStream, "gif")
            }
            if (data.type === Type.IMG) {
                val sticker = getBackgroundImage(File(key), data, avatarList, textList)!!
                val inputStream: InputStream = BaseImageMaker.makeImage(
                    avatarList, textList, sticker, antialias, gifMaxSize, encoder
                )!!
                return Pair<InputStream?, String>(inputStream, "png")
            }
        } catch (ex: Exception) {
            println("解析 $key/data.json 出错")
            ex.printStackTrace()
        }
        return null
    }

    /**
     * @return InputStream 及其图片格式（值域：["gif", "png"...]）
     */
    fun generateImage(
        key: String?, gifAvatarExtraDataProvider: GifAvatarExtraDataProvider?,
        textExtraData: TextExtraData?,
        additionTextDatas: List<TextData>?
    ): Pair<InputStream, String>? {
        var key = key
        if (!dataMap.containsKey(key) && !aliaMap.containsKey(key)) {
            println("无效的key: “$key”")
            return null
        }
        val data = if (dataMap.containsKey(key)) dataMap[key] else dataMap[aliaMap[key]!![0]]
        key = dataRoot!!.absolutePath + File.separator +
                (if (dataMap.containsKey(key)) key else aliaMap[key]) + File.separator
        val latch = CountDownLatch(
            data!!.text.size + data.avatar.size
                    + (additionTextDatas?.size ?: 0)
        )
        try {
            val textList: ArrayList<TextModel> = ArrayList<TextModel>()
            // add from KeyData
            if (!data.text.isEmpty()) {
                for (textElement in data.text) {
                    Thread {
                        textList.add(TextModel(textElement, textExtraData))
                        latch.countDown()
                    }.start()
                }
            }
            // add from params
            if (additionTextDatas != null) {
                for (textElement in additionTextDatas) {
                    Thread {
                        textList.add(TextModel(textElement, textExtraData))
                        latch.countDown()
                    }.start()
                }
            }
            val avatarList: ArrayList<AvatarModel> = ArrayList<AvatarModel>()
            if (!data.avatar.isEmpty()) {
                for (avatarData in data.avatar) {
                    Thread {
                        avatarList.add(AvatarModel(avatarData, gifAvatarExtraDataProvider!!, data.type))
                        latch.countDown()
                    }.start()
                }
            }
            val delay: Int = if (data.delay != null) data.delay else 65
            if (data.type === Type.GIF) {
                val stickerMap = HashMap<Short, BufferedImage>()
                var imageNum: Short = 0
                for (file in Objects.requireNonNull(File(key).listFiles())) {
                    if (!file.name.endsWith(".png")) continue
                    stickerMap[imageNum] = ImageIO.read(File(key + imageNum++ + ".png"))
                }
                latch.await()
                if (data.background != null) { //从配置文件读背景
                    val sticker: BufferedImage = BackgroundModel(data.background, avatarList, textList).getImage()
                    for (i in 0 until avatarList[0].posLength) {
                        stickerMap[i.toShort()] = sticker
                    }
                }
                val inputStream: InputStream = BaseGifMaker.makeGIF(
                    avatarList, textList, stickerMap, antialias, gifMaxSize, encoder, delay
                )!!
                return Pair(inputStream, "gif")
            }
            if (data.type === Type.IMG) {
                latch.await()
                val sticker = getBackgroundImage(File(key), data, avatarList, textList)!!
                val inputStream: InputStream = BaseImageMaker.makeImage(
                    avatarList, textList, sticker, antialias, gifMaxSize, encoder
                )!!
                return Pair(inputStream, "png")
            }
        } catch (ex: Exception) {
            println("解析 $key/data.json 出错")
            ex.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun getBackgroundImage(
        path: File, data: KeyData?,
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>
    ): BufferedImage? {
        if (!path.isDirectory || !path.exists()) return null
        val files = path.listFiles()!!
        val fileList = Arrays.stream(files).filter { file: File -> file.name.endsWith(".png") }
            .collect(Collectors.toList())
        if (fileList.isEmpty() && data!!.background == null) { //没有背景图片和背景配置
            throw FileNotFoundException("找不到" + path.name + "背景文件")
        }
        if (fileList.isEmpty() && data!!.background != null) { //无背景图片(读取背景配置
            return BackgroundModel(data.background, avatarList, textList).getImage()
        }
        return if (data!!.background == null) { //无背景配置(读取随机背景图片
            ImageIO.read(fileList[Random().nextInt(fileList.size)])
        } else BackgroundModel(
            data.background, avatarList, textList,
            ImageIO.read(fileList[Random().nextInt(fileList.size)])
        ).getImage()
        //有配置项和图片
    }

    fun setGifMaxSize(maxSize: MutableList<Int?>?) {
        if (maxSize == null || maxSize.isEmpty()) return
        if (maxSize.size > 3) {
            println("GifMaxSize无效: Length Must <= 3")
            return
        }
        if (maxSize.size == 1) maxSize.add(maxSize[0])
        if (maxSize.size == 2) maxSize.add(null)
        gifMaxSize = maxSize.stream()
            .map(Function<Int?, Int?> { i: Int? ->if (i != null && i <= 0) null else i })
            .collect(Collectors.toList())
    }

    companion object {
        const val FONTS_FOLDER = "fonts"
        var keyListString: String? = null
        @Throws(IOException::class)
        fun getFileStr(file: File?): String {
            val br = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
            val sb = StringBuilder()
            var str: String?
            while (br.readLine().also { str = it } != null) {
                sb.append(str)
            }
            br.close()
            return sb.toString()
        }

        @JvmOverloads
        fun decodeColor(jsonElement: JsonElement?, defaultRgba: ShortArray = shortArrayOf(255, 255, 255, 255)): Color {
            if (jsonElement == null) return Color(
                defaultRgba[0].toInt(),
                defaultRgba[1].toInt(),
                defaultRgba[2].toInt(),
                defaultRgba[3].toInt()
            )
            assert(defaultRgba.size == 4)
            try { //rgb or rgba
                val jsonArray = jsonElement as JsonArray
                if (jsonArray.size == 3 || jsonArray.size == 4) {
                    defaultRgba[0] = jsonArray[0].toString().toShort()
                    defaultRgba[1] = jsonArray[1].toString().toShort()
                    defaultRgba[2] = jsonArray[2].toString().toShort()
                    defaultRgba[3] = if (jsonArray.size == 4) jsonArray[3].toString().toShort() else 255
                }
            } catch (ignored: Exception) { //hex
                val hex = jsonElement.toString().replace("#", "").replace("\"", "")
                if (hex.length != 6 && hex.length != 8) {
                    println("颜色格式有误，请输入正确的16进制颜色\n输入: $hex")
                    return Color(
                        defaultRgba[0].toInt(),
                        defaultRgba[1].toInt(),
                        defaultRgba[2].toInt(),
                        defaultRgba[3].toInt()
                    )
                }
                defaultRgba[0] = hex.substring(0, 2).toShort(16)
                defaultRgba[1] = hex.substring(2, 4).toShort(16)
                defaultRgba[2] = hex.substring(4, 6).toShort(16)
                defaultRgba[3] = if (hex.length == 8) hex.substring(6, 8).toShort(16) else 255
            }
            return Color(defaultRgba[0].toInt(), defaultRgba[1].toInt(), defaultRgba[2].toInt(), defaultRgba[3].toInt())
        }
    }
}