package command.common.group.funny.petpet.share

import xmmt.dituon.share.ImageDeformer
import xmmt.dituon.share.ReusableGifDecoder
import xmmt.dituon.share.TextModel
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.net.URL
import java.util.stream.Collectors
import javax.imageio.ImageIO

abstract class ImageSynthesisCore {
    companion object {
        /**
         * 在Graphics2D画布上 绘制缩放头像
         *
         * @param g2d         Graphics2D 画布
         * @param avatarImage 处理后的头像
         * @param pos         处理后的坐标 (int[4]{x, y, w, h})
         * @param angle       旋转角, 对特殊角度有特殊处理分支
         * @param isRound     裁切为圆形
         * @param multiple    缩放倍数
         */
        /**
         * 在Graphics2D画布上 绘制缩放头像
         *
         * @param g2d         Graphics2D 画布
         * @param avatarImage 处理后的头像
         * @param pos         处理后的坐标 (int[4]{x, y, w, h})
         * @param angle       旋转角, 对特殊角度有特殊处理分支
         * @param isRound     裁切为圆形
         */
        @JvmStatic
        protected fun g2dDrawZoomAvatar(
            g2d: Graphics2D, avatarImage: BufferedImage?, pos: IntArray,
            angle: Float, isRound: Boolean, multiple: Float = 1.0f
        ) {
            if (avatarImage == null) {
                return
            }
            val x = (pos[0] * multiple).toInt()
            val y = (pos[1] * multiple).toInt()
            val w = (pos[2] * multiple).toInt()
            val h = (pos[3] * multiple).toInt()
            if (angle == 0f) {
                g2d.drawImage(avatarImage, x, y, w, h, null)
                return
            }
            if (isRound || angle % 90 == 0f) {
                val newAvatarImage = BufferedImage(avatarImage.width, avatarImage.height, avatarImage.type)
                val rotateG2d = newAvatarImage.createGraphics()
                rotateG2d.rotate(
                    Math.toRadians(angle.toDouble()),
                    (avatarImage.width / 2).toDouble(),
                    (avatarImage.height / 2).toDouble()
                )
                rotateG2d.drawImage(avatarImage, null, 0, 0)
                rotateG2d.dispose()
                g2d.drawImage(newAvatarImage, x, y, w, h, null)
                return
            }
            g2d.drawImage(rotateImage(avatarImage, angle), x, y, w, h, null)
        }

        /**
         * 在Graphics2D画布上 绘制变形头像
         *
         * @param g2d         Graphics2D 画布
         * @param avatarImage 处理后的头像
         * @param deformPos   头像四角坐标 (Point2D[4]{左上角, 左下角, 右下角, 右上角})
         * @param anchorPos   锚点坐标
         */
        @JvmStatic
        protected fun g2dDrawDeformAvatar(
            g2d: Graphics2D, avatarImage: BufferedImage,
            deformPos: Array<Point2D?>, anchorPos: IntArray
        ) {
            val result: BufferedImage = ImageDeformer.computeImage(avatarImage, deformPos)
            g2d.drawImage(result, anchorPos[0], anchorPos[1], null)
        }

        /**
         * 在Graphics2D画布上 绘制变形头像
         *
         * @param g2d         Graphics2D 画布
         * @param avatarImage 处理后的头像
         * @param deformPos   头像四角坐标 (Point2D[4]{左上角, 左下角, 右下角, 右上角})
         * @param anchorPos   锚点坐标
         * @param multiple    缩放倍数
         */
        protected fun g2dDrawDeformAvatar(
            g2d: Graphics2D, avatarImage: BufferedImage,
            deformPos: Array<Point2D?>, anchorPos: IntArray, multiple: Float
        ) {
            for (point in deformPos) {
                point!!.setLocation(point.x * multiple, point.y * multiple)
            }
            val result: BufferedImage = ImageDeformer.computeImage(avatarImage, deformPos)
            g2d.drawImage(result, (anchorPos[0] * multiple).toInt(), (anchorPos[1] * multiple).toInt(), null)
        }

        /**
         * 在Graphics2D画布上 绘制文字
         *
         * @param g2d   Graphics2D 画布
         * @param text  文本数据
         * @param pos   坐标 (int[2]{x, y})
         * @param color 颜色
         * @param font  字体
         */
        fun g2dDrawText(g2d: Graphics2D, text: String, pos: IntArray, color: Color?, font: Font) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.color = color
            g2d.font = font
            if (text.contains("\n")) {
                val texts = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var y = pos[1]
                val height: Short = TextModel.getTextHeight(text, font).toShort()
                for (txt in texts) {
                    g2d.drawString(txt, pos[0], y)
                    y += height.toInt()
                }
                return
            }
            g2d.drawString(text, pos[0], pos[1])
        }

        /**
         * 将图像裁切为圆形
         *
         * @param input     输入图像
         * @param antialias 抗锯齿
         * @return 裁切后的图像
         */
        fun convertCircular(input: BufferedImage, antialias: Boolean): BufferedImage {
            val output = BufferedImage(input.width, input.height, BufferedImage.TYPE_4BYTE_ABGR)
            val shape = Ellipse2D.Double(0.0, 0.0, input.width.toDouble(), input.height.toDouble())
            val g2 = output.createGraphics()
            g2.clip = shape
            if (antialias) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            }
            g2.drawImage(input, 0, 0, null)
            g2.dispose()
            return output
        }

        /**
         * 将图像裁切为圆形
         *
         * @param inputList 输入图像数组
         * @param antialias 抗锯齿
         * @return 裁切后的图像
         */
        fun convertCircular(inputList: List<BufferedImage>?, antialias: Boolean): MutableList<BufferedImage> {
            return inputList!!.stream()
                .map { input: BufferedImage -> convertCircular(input, antialias) }
                .collect(Collectors.toList())
        }

        /**
         * 完整旋转图像 (旋转时缩放以保持图像完整性)
         *
         * @param avatarImage 输入图像
         * @param angle       旋转角度
         * @return 旋转后的图像
         */
        fun rotateImage(avatarImage: BufferedImage, angle: Float): BufferedImage {
            val sin = Math.abs(Math.sin(Math.toRadians(angle.toDouble())))
            val cos = Math.abs(Math.cos(Math.toRadians(angle.toDouble())))
            val w = avatarImage.width
            val h = avatarImage.height
            val neww = Math.floor(w * cos + h * sin).toInt()
            val newh = Math.floor(h * cos + w * sin).toInt()
            var rotated = BufferedImage(neww, newh, avatarImage.type)
            var g2d = rotated.createGraphics()
            rotated = g2d.deviceConfiguration.createCompatibleImage(
                rotated.width, rotated.height, Transparency.TRANSLUCENT
            )
            g2d.dispose()
            g2d = rotated.createGraphics()
            g2d.translate((neww - w) / 2, (newh - h) / 2)
            g2d.rotate(Math.toRadians(angle.toDouble()), (w / 2).toDouble(), (h / 2).toDouble())
            g2d.drawRenderedImage(avatarImage, null)
            g2d.dispose()
            return rotated
        }

        /**
         * 从URL获取网络图像
         *
         * @param imageUrl 图像URL
         */
        fun getWebImage(imageUrl: String): BufferedImage {
            var image: BufferedImage? = null
            try {
                image = ImageIO.read(URL(imageUrl))
            } catch (e: Exception) {
                println("[获取图像失败]  URL: $imageUrl")
                e.printStackTrace()
            }
            return image!!
        }

        /**
         * 从URL获取网络图像 (支持GIF)
         *
         * @param imageUrl 图像URL
         * @return GIF全部帧 或一张静态图像
         */
        fun getWebImageAsList(imageUrl: String): List<BufferedImage> {
            val output = ArrayList<BufferedImage>()
            try {
                val url = URL(imageUrl)
                val decoder = ReusableGifDecoder()
                val inputStream = BufferedInputStream(url.openStream())
                inputStream.mark(0) //循环利用inputStream, 避免重复获取
                decoder.read(inputStream)
                if (decoder.err()) {
                    inputStream.reset()
                    output.add(ImageIO.read(ImageIO.createImageInputStream(inputStream)))
                    inputStream.close()
                    return output
                }
                inputStream.close()
                for (i in 0 until decoder.getFrameCount()) {
                    output.add(decoder.getFrame(i))
                }
            } catch (ex: Exception) {
                println("[获取/解析 图像失败]  URL: $imageUrl")
                ex.printStackTrace()
            }
            return output
        }
        /**
         * 裁切图像
         *
         * @param image     输入图像
         * @param cropPos   裁切坐标 (int[4]{x1, y1, x2, y2})
         * @param isPercent 按百分比处理坐标
         * @return 裁切后的图像
         */
        /**
         * 裁切图像
         *
         * @param image   输入图像
         * @param cropPos 裁切坐标 (int[4]{x1, y1, x2, y2})
         * @return 裁切后的图像
         */
        @JvmOverloads
        fun cropImage(image: BufferedImage, cropPos: IntArray, isPercent: Boolean = false): BufferedImage {
            var width = cropPos[2] - cropPos[0]
            var height = cropPos[3] - cropPos[1]
            if (isPercent) {
                width = (width.toFloat() / 100 * image.width).toInt()
                height = (height.toFloat() / 100 * image.height).toInt()
            }
            val croppedImage = BufferedImage(width, height, image.type)
            val g2d = croppedImage.createGraphics()
            if (isPercent) { //百分比
                g2d.drawImage(
                    image,
                    0,
                    0,
                    width,
                    height,
                    (cropPos[0].toFloat() / 100 * image.width).toInt(),
                    (cropPos[1].toFloat() / 100 * image.height).toInt(),
                    (cropPos[2].toFloat() / 100 * image.width).toInt(),
                    (cropPos[3].toFloat() / 100 * image.height).toInt(),
                    null
                )
            } else { //像素
                g2d.drawImage(
                    image, 0, 0, width, height, cropPos[0], cropPos[1], cropPos[2], cropPos[3], null
                )
            }
            g2d.dispose()
            return croppedImage
        }

        /**
         * 裁切图像
         *
         * @param imageList 输入图像数组
         * @param cropPos   裁切坐标 (int[4]{x1, y1, x2, y2})
         * @param isPercent 按百分比处理坐标
         * @return 裁切后的图像
         */
        fun cropImage(
            imageList: List<BufferedImage>?,
            cropPos: IntArray, isPercent: Boolean
        ): MutableList<BufferedImage> {
            return imageList!!.stream()
                .map { image: BufferedImage -> cropImage(image, cropPos, isPercent) }
                .collect(Collectors.toList())
        }

        /**
         * 镜像翻转图像
         */
        fun mirrorImage(image: BufferedImage): BufferedImage {
            val width = image.width
            val height = image.height
            var mirroredImage: BufferedImage
            var g2d: Graphics2D
            BufferedImage(
                width, height, image
                    .colorModel.transparency
            ).also { mirroredImage = it }.createGraphics().also { g2d = it }
                .drawImage(image, 0, 0, width, height, width, 0, 0, height, null)
            g2d.dispose()
            return mirroredImage
        }

        /**
         * 镜像翻转图像数组
         */
        fun mirrorImage(imageList: List<BufferedImage>?): MutableList<BufferedImage> {
            return imageList!!.stream().map { image: BufferedImage -> mirrorImage(image) }
                .collect(Collectors.toList())
        }

        /**
         * 竖直翻转图像
         */
        fun flipImage(image: BufferedImage): BufferedImage {
            val flipped = BufferedImage(
                image.width, image.height,
                image.type
            )
            val tran = AffineTransform.getTranslateInstance(
                0.0,
                image.height.toDouble()
            )
            val flip = AffineTransform.getScaleInstance(1.0, -1.0)
            tran.concatenate(flip)
            val g2d = flipped.createGraphics()
            g2d.transform = tran
            g2d.drawImage(image, 0, 0, null)
            g2d.dispose()
            return flipped
        }

        /**
         * 竖直翻转图像数组
         */
        fun flipImage(imageList: List<BufferedImage>?): MutableList<BufferedImage> {
            return imageList!!.stream()
                .map { image: BufferedImage -> flipImage(image) }
                .collect(Collectors.toList())
        }

        /**
         * 图像灰度化
         */
        fun grayImage(image: BufferedImage): BufferedImage {
            val width = image.width
            val height = image.height
            val grayscaleImage = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = Color(image.getRGB(x, y))
                    val gray = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114).toInt()
                    val color_end = Color(gray, gray, gray)
                    grayscaleImage.setRGB(x, y, color_end.rgb)
                }
            }
            return grayscaleImage
        }

        /**
         * 灰度化图像数组
         */
        fun grayImage(imageList: List<BufferedImage>?): MutableList<BufferedImage> {
            return imageList!!.stream().map { image: BufferedImage -> grayImage(image) }
                .collect(Collectors.toList())
        }

        /**
         * 图像二值化
         */
        fun binarizeImage(image: BufferedImage): BufferedImage {
            val h = image.height
            val w = image.width
            val binarizeImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            for (i in 0 until w) {
                for (j in 0 until h) {
                    val `val` = image.getRGB(i, j)
                    val r = 0x00ff0000 and `val` shr 16
                    val g = 0x0000ff00 and `val` shr 8
                    val b = 0x000000ff and `val`
                    val m = r + g + b
                    if (m >= 383) {
                        binarizeImage.setRGB(i, j, Color.WHITE.rgb)
                    } else {
                        binarizeImage.setRGB(i, j, 0)
                    }
                }
            }
            return binarizeImage
        }

        /**
         * 二值化图像数组
         */
        fun binarizeImage(imageList: List<BufferedImage>?): MutableList<BufferedImage> {
            return imageList!!.stream().map { image: BufferedImage -> binarizeImage(image) }
                .collect(Collectors.toList())
        }

        /**
         * BufferedImage转为int[][]数组
         */
        fun convertImageToArray(bf: BufferedImage): Array<IntArray> {
            val width = bf.width
            val height = bf.height
            val data = IntArray(width * height)
            bf.getRGB(0, 0, width, height, data, 0, width)
            val rgbArray = Array(height) { IntArray(width) }
            for (i in 0 until height) System.arraycopy(data, i * width, rgbArray[i], 0, width)
            return rgbArray
        }
    }
}