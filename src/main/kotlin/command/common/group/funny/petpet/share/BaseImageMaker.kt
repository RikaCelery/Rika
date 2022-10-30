package xmmt.dituon.share

import command.common.group.funny.petpet.share.AvatarModel
import command.common.group.funny.petpet.share.Encoder
import xmmt.dituon.share.ImageSynthesis.synthesisImage
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

object BaseImageMaker {
    fun makeImage(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>,
        sticker: BufferedImage?, antialias: Boolean, encoder: Encoder
    ): InputStream? {
        return makeImage(avatarList, textList, sticker, antialias, null, encoder)
    }

    fun makeImage(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>,
        sticker: BufferedImage?, antialias: Boolean, maxSize: List<Int?>?,
        encoder: Encoder
    ): InputStream? {
        for (avatar in avatarList) {
            if (avatar.isGif) return BaseGifMaker.makeGIF(
                avatarList, textList, sticker, antialias, maxSize, encoder, 65
            )
        }
        try {
            return bufferedImageToInputStream(
                synthesisImage(
                    sticker, avatarList, textList, antialias, true
                )
            )
        } catch (e: IOException) {
            println("构造IMG失败，请检查 PetData")
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun bufferedImageToInputStream(bf: BufferedImage): InputStream {
        val os = ByteArrayOutputStream()
        ImageIO.write(bf, "png", os)
        return ByteArrayInputStream(os.toByteArray())
    }
}