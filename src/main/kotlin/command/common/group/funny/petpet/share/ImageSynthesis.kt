package xmmt.dituon.share

import command.common.group.funny.petpet.share.AvatarModel
import command.common.group.funny.petpet.share.AvatarModel.DeformData
import command.common.group.funny.petpet.share.AvatarPosType
import command.common.group.funny.petpet.share.CropType
import command.common.group.funny.petpet.share.ImageSynthesisCore
import java.awt.*
import java.awt.image.BufferedImage
import java.util.function.Consumer

object ImageSynthesis : ImageSynthesisCore() {
    internal fun g2dDrawAvatar(
        g2d: Graphics2D, avatar: AvatarModel,
        index: Short, multiple: Float = 1.0f
    ) {
        when (avatar.posType) {
            AvatarPosType.ZOOM -> g2dDrawZoomAvatar(
                g2d, avatar.getFrame(index), avatar.getPos(index),
                avatar.getAngle(index), avatar.isRound, multiple
            )
            AvatarPosType.DEFORM -> {
                val deformData: DeformData = avatar.deformData!!
                ImageSynthesisCore.Companion.g2dDrawDeformAvatar(
                    g2d, avatar.getFrame(index),
                    deformData.deformPos, deformData.anchor
                )
            }
        }
    }

    internal fun g2dDrawTexts(
        g2d: Graphics2D, texts: ArrayList<TextModel>?,
        stickerWidth: Int, stickerHeight: Int
    ) {
        if (texts == null || texts.isEmpty()) return
        texts.forEach(Consumer<TextModel> { text: TextModel -> text.drawAsG2d(g2d, stickerWidth, stickerHeight) })
    }

    @JvmOverloads
    fun synthesisImage(
        sticker: BufferedImage?, avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        antialias: Boolean, transparent: Boolean = false, index: Short = 0, maxSize: List<Int?>? = null
    ): BufferedImage {
        var stickerWidth = sticker!!.width
        var stickerHeight = sticker.height
        var multiple = 1.0f
        if (maxSize != null && !maxSize.isEmpty()) {
            var zoom = false
            if (maxSize[2] != null) {
                for (avatar in avatarList) {
                    if (avatar.imageList!!.size >= maxSize[2]!!) {
                        zoom = true
                        break
                    }
                }
            }
            if (zoom) {
                if (stickerWidth > maxSize[0]!!) multiple = maxSize[0]!!.toFloat() / sticker.width
                if (stickerHeight > maxSize[1]!!) multiple = Math.min(
                    multiple, maxSize[1]!!
                        .toFloat() / sticker.height
                )
                stickerWidth = (stickerWidth * multiple).toInt()
                stickerHeight = (stickerHeight * multiple).toInt()
            }
        }
        var output = BufferedImage(stickerWidth, stickerHeight, sticker.type)
        var g2d = output.createGraphics()
        if (antialias) { //抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }

        // 背景
        if (transparent) {
            output = g2d.deviceConfiguration.createCompatibleImage(
                stickerWidth, stickerHeight, Transparency.TRANSLUCENT
            )
            g2d.dispose()
            g2d = output.createGraphics()
        } else {
            g2d.color = Color.WHITE
            g2d.fillRect(0, 0, stickerWidth, stickerHeight)
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f)
        }

        // 按照图层分类
        val topAvatars: ArrayList<AvatarModel> = ArrayList<AvatarModel>()
        val bottomAvatars: ArrayList<AvatarModel> = ArrayList<AvatarModel>()
        for (avatar in avatarList) {
            if (avatar.isOnTop) {
                topAvatars.add(avatar)
            } else {
                bottomAvatars.add(avatar)
            }
        }
        // 画
        for (avatar in bottomAvatars) {
            g2dDrawAvatar(g2d, avatar, index, multiple)
        }
        g2d.drawImage(sticker, 0, 0, stickerWidth, stickerHeight, null)
        for (avatar in topAvatars) {
            g2dDrawAvatar(g2d, avatar, index, multiple)
        }
        g2dDrawTexts(g2d, textList, stickerWidth, stickerHeight)
        g2d.dispose()
        return output
    }

    fun cropImage(image: BufferedImage, type: CropType, cropPos: IntArray): BufferedImage {
        return ImageSynthesisCore.Companion.cropImage(image, cropPos, type === CropType.PERCENT)
    }

    fun cropImage(imageList: List<BufferedImage>?, type: CropType?, cropPos: IntArray): MutableList<BufferedImage> {
        return ImageSynthesisCore.Companion.cropImage(imageList, cropPos, type === CropType.PERCENT)
    }
}