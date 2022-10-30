package xmmt.dituon.share

import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.Image
import com.squareup.gifencoder.ImageOptions
import command.common.group.funny.petpet.share.AvatarModel
import command.common.group.funny.petpet.share.BufferedGifEncoder
import command.common.group.funny.petpet.share.Encoder
import command.common.group.funny.petpet.share.ImageSynthesisCore
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object BaseGifMaker {
    fun makeGIF(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>, antialias: Boolean
    ): InputStream? {
        return makeGifUseBufferedStream(avatarList, textList, stickerMap, antialias, null, 65)
    }

    fun makeGIF(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>,
        antialias: Boolean, encoder: Encoder, delay: Int
    ): InputStream? {
        return makeGIF(avatarList, textList, stickerMap, antialias, null, encoder, delay)
    }

    fun makeGIF(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>,
        antialias: Boolean, maxSize: List<Int?>?,
        encoder: Encoder, delay: Int
    ): InputStream? {
        if (encoder === Encoder.BUFFERED_STREAM) {
            return makeGifUseBufferedStream(avatarList, textList, stickerMap, antialias, maxSize, delay)
        }
        if (encoder === Encoder.ANIMATED_LIB) {
            return makeGifUseAnimatedLib(avatarList, textList, stickerMap, antialias, maxSize, delay)
        }
        return if (encoder === Encoder.SQUAREUP_LIB) {
            makeGifUseSquareupLib(avatarList, textList, stickerMap, antialias, maxSize, delay)
        } else null
    }

    fun makeGifUseBufferedStream(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>,
        antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream? {
        return try {
            //遍历获取GIF长度(图片文件数量)
            var i: Short = 0
            val latch = CountDownLatch(stickerMap.size)
            val imageMap = HashMap<Short, BufferedImage>()
            for (key in stickerMap.keys) {
                val fi = i++
                Thread {
                    imageMap[fi] = ImageSynthesis.synthesisImage(
                        stickerMap[key], avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    latch.countDown()
                }.start()
            }
            val gifEncoder = BufferedGifEncoder(stickerMap[0.toShort()]!!.type, delay, true)
            latch.await()
            i = 0
            while (i < imageMap.size) {
                gifEncoder.addFrame(imageMap[i])
                i++
            }
            gifEncoder.finish()
            gifEncoder.output
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun makeGifUseAnimatedLib(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>,
        antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream {
        return try {
            //遍历获取GIF长度(图片文件数量)
            var i: Short = 0
            val latch = CountDownLatch(stickerMap.size)
            val imageMap = HashMap<Short, BufferedImage>()
            for (key in stickerMap.keys) {
                val fi = i++
                Thread {
                    imageMap[fi] = ImageSynthesis.synthesisImage(
                        stickerMap[key], avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    latch.countDown()
                }.start()
            }
            val gifEncoder = AnimatedGifEncoder()
            val output = ByteArrayOutputStream()
            gifEncoder.start(output)
            gifEncoder.setRepeat(0)
            gifEncoder.setDelay(delay)
            latch.await()
            imageMap.forEach { (`in`: Short?, image: BufferedImage?) -> gifEncoder.addFrame(image) }
            gifEncoder.finish()
            ByteArrayInputStream(output.toByteArray())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun makeGifUseSquareupLib(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        stickerMap: HashMap<Short, BufferedImage>,
        antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream {
        return try {
            var i: Short = 0
            val latch = CountDownLatch(stickerMap.size)
            val imageMap = HashMap<Short, Image>()
            val size = IntArray(2)
            for (key in stickerMap.keys) {
                val fi = i++
                Thread {
                    val image: BufferedImage = ImageSynthesis.synthesisImage(
                        stickerMap[key], avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    if (fi.toInt() == 0) {
                        size[0] = image.width
                        size[1] = image.height
                    }
                    val rgb = Image.fromRgb(ImageSynthesisCore.Companion.convertImageToArray(image))
                    imageMap[fi] = rgb
                    latch.countDown()
                }.start()
            }
            val output = ByteArrayOutputStream()
            val options = ImageOptions().setDelay(delay.toLong(), TimeUnit.MILLISECONDS)
            latch.await()
            val gifEncoder = GifEncoder(output, size[0], size[1], 0)
            imageMap.forEach { (`in`: Short?, image: Image?) ->
                try {
                    gifEncoder.addImage(image, options)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            gifEncoder.finishEncoding()
            ByteArrayInputStream(output.toByteArray())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun makeGIF(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>,
        sticker: BufferedImage?, antialias: Boolean, delay: Int
    ): InputStream? {
        return makeGifUseBufferedStream(avatarList, textList, sticker, antialias, null, delay)
    }

    fun makeGIF(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>,
        sticker: BufferedImage?,
        antialias: Boolean, maxSize: List<Int?>?, encoder: Encoder, delay: Int
    ): InputStream? {
        if (encoder === Encoder.BUFFERED_STREAM) {
            return makeGifUseBufferedStream(avatarList, textList, sticker, antialias, maxSize, delay)
        }
        if (encoder === Encoder.ANIMATED_LIB) {
            return makeGifUseAnimatedLib(avatarList, textList, sticker, antialias, maxSize, delay)
        }
        return if (encoder === Encoder.SQUAREUP_LIB) {
            makeGifUseSquareupLib(avatarList, textList, sticker, antialias, maxSize, delay)
        } else null
    }

    private fun makeGifUseBufferedStream(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>,
        sticker: BufferedImage?, antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream? {
        return try {
            var maxFrameLength: Short = 1
            for (avatar in avatarList) {
                maxFrameLength = Math.max(maxFrameLength.toInt(), avatar.imageList!!.size).toShort()
            }
            val latch = CountDownLatch(maxFrameLength.toInt())
            val imageMap = HashMap<Short, BufferedImage>()
            for (i in 0 until maxFrameLength) {
                val fi = i.toShort()
                Thread {
                    imageMap[fi] = ImageSynthesis.synthesisImage(
                        sticker, avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    latch.countDown()
                }.start()
            }
            val gifEncoder = BufferedGifEncoder(sticker!!.type, delay, true)
            latch.await()
            for (i in 0 until imageMap.size) gifEncoder.addFrame(imageMap.get(i.toShort()))
            gifEncoder.finish()
            gifEncoder.output
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun makeGifUseAnimatedLib(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        sticker: BufferedImage?, antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream {
        return try {
            var maxFrameLength: Short = 1
            for (avatar in avatarList) {
                maxFrameLength = Math.max(maxFrameLength.toInt(), avatar.imageList!!.size).toShort()
            }
            val latch = CountDownLatch(maxFrameLength.toInt())
            val imageMap = HashMap<Short, BufferedImage>()
            for (i in 0 until maxFrameLength) {
                val fi = i.toShort()
                Thread {
                    imageMap[fi] = ImageSynthesis.synthesisImage(
                        sticker, avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    latch.countDown()
                }.start()
            }
            val gifEncoder = AnimatedGifEncoder()
            val output = ByteArrayOutputStream()
            gifEncoder.start(output)
            gifEncoder.setDelay(delay)
            gifEncoder.setRepeat(0)
            latch.await()
            imageMap.forEach { (i: Short?, image: BufferedImage?) -> gifEncoder.addFrame(image) }
            gifEncoder.finish()
            ByteArrayInputStream(output.toByteArray())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun makeGifUseSquareupLib(
        avatarList: ArrayList<AvatarModel>, textList: ArrayList<TextModel>?,
        sticker: BufferedImage?,
        antialias: Boolean, maxSize: List<Int?>?, delay: Int
    ): InputStream {
        return try {
            var i: Short = 0
            var maxFrameLength: Short = 1
            for (avatar in avatarList) {
                maxFrameLength = Math.max(maxFrameLength.toInt(), avatar.imageList!!.size).toShort()
            }
            val latch = CountDownLatch(maxFrameLength.toInt())
            val imageMap = HashMap<Short, Image>()
            val size = IntArray(2)
            i = 0
            while (i < maxFrameLength) {
                val fi = i
                Thread {
                    val image: BufferedImage = ImageSynthesis.synthesisImage(
                        sticker, avatarList, textList,
                        antialias, false, fi, maxSize
                    )
                    if (fi.toInt() == 0) {
                        size[0] = image.width
                        size[1] = image.height
                    }
                    val rgb = Image.fromRgb(ImageSynthesisCore.Companion.convertImageToArray(image))
                    imageMap[fi] = rgb
                    latch.countDown()
                }.start()
                i++
            }
            val output = ByteArrayOutputStream()
            val options = ImageOptions().setDelay(delay.toLong(), TimeUnit.MILLISECONDS)
            latch.await()
            val gifEncoder = GifEncoder(
                output,
                size[0], size[1], 0
            )
            imageMap.forEach { (`in`: Short?, image: Image?) ->
                try {
                    gifEncoder.addImage(image, options)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            gifEncoder.finishEncoding()
            ByteArrayInputStream(output.toByteArray())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}