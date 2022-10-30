package command.common.group.funny.petpet.share

import kotlinx.serialization.json.JsonArray
import xmmt.dituon.share.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.util.*

class AvatarModel {
    private var imageType: Type? = null
    var type: AvatarType? = null
    var pos = arrayOf(intArrayOf(0, 0, 100, 100))
    var angle: Short = 0
    var isRound = false
        set
    var isRotate = false
        set
    var isOnTop = false
        set
    var imageList: MutableList<BufferedImage>? = null
    private var posIndex: Short = 0
    var isAntialias = false
        private set
    var posType: AvatarPosType? = null
        private set
    var deformData: DeformData? = null
        private set
    private var cropType: CropType? = null
    private var cropPos: IntArray = IntArray(0)
    private var styleList: List<AvatarStyle>? = null
    private var frameIndex: Short = 0

    @Deprecated("")
    constructor(data: AvatarData, extraData: AvatarExtraDataProvider, imageType: Type) {
        setImage(data.type, extraData)
        buildData(data, imageType)
    }

    constructor(data: AvatarData, extraData: GifAvatarExtraDataProvider, imageType: Type) {
        setImage(data.type, extraData)
        buildData(data, imageType)
    }

    private fun buildData(data: AvatarData, imageType: Type) {
        type = data.type
        posType = data.posType ?: AvatarPosType.ZOOM
        setPos(data.pos, imageType.also { this.imageType = it })
        cropType = data.cropType
        setCrop(data.crop)
        styleList = data.style
        angle = data.angle ?: 0
        isRound = java.lang.Boolean.TRUE == data.round
        isRotate = java.lang.Boolean.TRUE == data.rotate
        isOnTop = java.lang.Boolean.TRUE == data.avatarOnTop
        isAntialias = java.lang.Boolean.TRUE == data.antialias
        buildImage()
    }

    private fun setImage(type: AvatarType, extraData: AvatarExtraDataProvider) {
        imageList = mutableListOf()
        when (type) {
            AvatarType.FROM -> imageList!!.add(
                extraData.fromAvatar!!()
            )
            AvatarType.TO -> imageList!!.add(
                (extraData.toAvatar)!!()
            )
            AvatarType.GROUP -> imageList!!.add(
                (extraData.groupAvatar)!!()
            )
            AvatarType.BOT -> imageList!!.add(
                (extraData.botAvatar)!!()
            )
        }
    }

    private fun setImage(type: AvatarType, extraData: GifAvatarExtraDataProvider) {
        imageList = when (type) {
            AvatarType.FROM ->extraData.fromAvatar!!()
            AvatarType.TO ->extraData.toAvatar!!()
            AvatarType.GROUP ->extraData.groupAvatar!!()
            AvatarType.BOT ->extraData.botAvatar!!()
        } as MutableList<BufferedImage>
    }

    private fun setPos(posElements: JsonArray?, imageType: Type) {
        var i = 0
        when (posType) {
            AvatarPosType.ZOOM -> when (imageType) {
                Type.GIF -> {
                    pos = Array(posElements!!.size) { IntArray(4) }
                    for (je in posElements) {
                        val ja = je as JsonArray
                        if (ja.size != 4) {
                            return
                        }
                        pos[i++] = JsonArrayToIntArray(ja)
                    }
                }
                Type.IMG -> pos[i] = JsonArrayToIntArray(posElements)
            }
            AvatarPosType.DEFORM -> deformData = DeformData.fromPos(posElements)
        }
    }

    private fun setCrop(crop: JsonArray?) {
        if (crop == null || crop.isEmpty()) return
        val result = JsonArrayToIntArray(crop)
        cropPos = if (result.size == 2) intArrayOf(0, 0, result[0], result[1]) else result
    }

    private fun JsonArrayToIntArray(ja: JsonArray?): IntArray {
        val result = IntArray(ja!!.size)
        var i: Short = 0
        for (je in ja) {
            val str = je.toString().replace("\"", "")
            try {
                result[i.toInt()] = str.toInt()
            } catch (ignored: NumberFormatException) {
                val parser = ArithmeticParser(str)
                parser.put("width", imageWidth)
                parser.put("height", imageHeight)
                result[i.toInt()] = parser.eval().toInt()
            }
            i++
        }
        return result
    }

    private fun buildImage() {
        if (cropType !== CropType.NONE) imageList = ImageSynthesis.cropImage(imageList, cropType, cropPos)
        for (style in styleList!!) {
            imageList = when (style) {
                AvatarStyle.FLIP -> ImageSynthesisCore.flipImage(imageList)
                AvatarStyle.MIRROR -> ImageSynthesisCore.mirrorImage(imageList)
                AvatarStyle.GRAY -> ImageSynthesisCore.grayImage(imageList)
                AvatarStyle.BINARIZATION -> ImageSynthesisCore.binarizeImage(imageList)
            }
        }
        if (isRound) {
            imageList = ImageSynthesisCore.convertCircular(imageList, isAntialias)
        }
    }


    val firstImage: BufferedImage
        get() = imageList!![0]//不旋转
    //IMG随机旋转
    //GIF自动旋转
    /**
     * 获取下一个旋转角度
     *  * 不旋转时 返回初始角度
     *  * IMG格式 返回随机角度
     *  * GIF 返回下一个旋转角度
     *
     */
    @get:Deprecated("应当使用index直接获取角度, 之后的版本将不再维护posIndex")
    val nextAngle: Float
        get() {
            if (!isRotate) return angle.toFloat() //不旋转
            return if (imageType === Type.IMG) Random().nextInt((if (angle.toInt() != 0) angle else 360).toInt())
                .toFloat() else (360 / pos.size).toFloat() * posIndex + angle //IMG随机旋转
            //GIF自动旋转
        }

    /**
     * 获取旋转角度
     *  * 不旋转时 返回初始角度
     *  * IMG格式 返回随机角度
     *  * GIF 返回旋转角度
     */
    fun getAngle(index: Short): Float {
        if (!isRotate) return angle.toFloat() //不旋转
        return if (imageType === Type.IMG) Random().nextInt((if (angle.toInt() != 0) angle else 360).toInt())
            .toFloat() else (360 / pos.size).toFloat() * index + angle //IMG随机旋转
        //GIF自动旋转
    }

    /**
     * 获取下一个坐标
     *
     */
    @Deprecated("应当使用index直接获取坐标, 之后的版本将不再维护posIndex")
    fun nextPos(): IntArray {
        return getPos(posIndex++)
    }

    /**
     * 获取坐标(索引越界会返回最后的坐标)
     */
    fun getPos(i: Short): IntArray {
        return if (i >= pos.size) pos[pos.size - 1] else pos[i.toInt()]
    }

    /**
     * 获取坐标数组实际长度
     */
    val posLength: Short
        get() = pos.size.toShort()

    class DeformData {
        var deformPos = arrayOfNulls<Point2D>(POS_SIZE)
        var anchor = IntArray(2)

        companion object {
            const val POS_SIZE = 4
            fun fromPos(posElements: JsonArray?): DeformData {
//            System.out.println("DeformData fromPos by: " + posElements.toString());
                val deformData = DeformData()
                for (i in 0 until POS_SIZE) {
                    deformData.deformPos[i] = Point2D.Double(
                        (posElements!![i] as JsonArray)[0].toString().toInt()
                            .toDouble(), (posElements[i] as JsonArray)[1].toString().toInt()
                            .toDouble()
                    )
                }
                deformData.anchor[0] = (posElements!![POS_SIZE] as JsonArray)[0].toString().toInt()
                deformData.anchor[1] = (posElements[POS_SIZE] as JsonArray)[1].toString().toInt()
                return deformData
            }
        }
    }

    val imageWidth: Int
        get() = firstImage.width
    val imageHeight: Int
        get() = firstImage.height
    val isGif: Boolean
        get() = imageList!!.size > 1

    /**
     * 获取头像下一帧, 超过索引长度会重新开始循环 **(线程不安全)**
     * **应当使用index直接获取帧**
     */
    fun nextFrame(): BufferedImage {
        if (frameIndex >= imageList!!.size) frameIndex = 0
        return imageList!![frameIndex++.toInt()]
    }

    /**
     * 获取指定帧数, 超过索引长度会从头计数
     * 例如: length: 8 index: 10 return: list[1]
     */
    fun getFrame(i: Short): BufferedImage {
        var i = i
        i = (i % imageList!!.size).toShort()
        return imageList!![i.toInt()]
    }
}