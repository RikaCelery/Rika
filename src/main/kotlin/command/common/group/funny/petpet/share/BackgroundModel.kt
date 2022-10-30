package command.common.group.funny.petpet.share

import kotlinx.serialization.json.JsonArray
import xmmt.dituon.share.TextModel
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.image.BufferedImage

class BackgroundModel @JvmOverloads constructor(
    data: BackgroundData?,
    avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>, image: BufferedImage? = null
) {
    private val size: IntArray
    private var image: BufferedImage? = null
    private val color: Color

    init {
        size = JsonArrayToIntArray(data!!.size, avatarList, textList)
        this.image = image
        color = BasePetService.decodeColor(data.color, shortArrayOf(255, 255, 255, 255))
    }

    fun getImage(): BufferedImage {
        val output = BufferedImage(size[0], size[1], 1)
        val g2d = output.createGraphics()
        g2d.color = color
        g2d.fillRect(0, 0, size[0], size[1])
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f)
        if (image != null) g2d.drawImage(image, 0, 0, null)
        return output
    }

    private fun JsonArrayToIntArray(
        ja: JsonArray,
        avatarList: ArrayList<AvatarModel>,
        textList: ArrayList<TextModel>
    ): IntArray {
        val result = IntArray(ja.size)
        var i: Short = 0
        for (je in ja) {
            val str = je.toString().replace("\"", "")
            try {
                result[i.toInt()] = str.toInt()
            } catch (ignored: NumberFormatException) {
                val parser = ArithmeticParser(str)
                run {
                    var `in`: Short = 0
                    while (`in` < avatarList.size) {
                        parser.put("avatar" + `in` + "Width", avatarList[`in`.toInt()].imageWidth)
                        parser.put("avatar" + `in` + "Height", avatarList[`in`.toInt()].imageHeight)
                        `in`++
                    }
                }
                var `in`: Short = 0
                while (`in` < textList.size) {
                    parser.put(
                        "text" + `in` + "Width",
                        textList[`in`.toInt()].getWidth(textList[`in`.toInt()].getFont())
                    )
                    parser.put(
                        "text" + `in` + "Height",
                        textList[`in`.toInt()].getHeight(textList[`in`.toInt()].getFont())
                    )
                    `in`++
                }
                result[i.toInt()] = parser.eval().toInt()
            }
            i++
        }
        return result
    }
}