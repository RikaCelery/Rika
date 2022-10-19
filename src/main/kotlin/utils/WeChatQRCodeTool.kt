package org.celery.utils

import net.mamoe.mirai.utils.MiraiInternalApi
import org.celery.Rika
import org.celery.utils.file.createParentFolder
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.wechat_qrcode.WeChatQRCode
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.File

/**
 * @program: test-wechatqrcode
 * @description: 微信二维码扫描opencv库方法调用
 * @author: test
 * @create: 221-08-21 17:05
 * 原创点赞：java+opencv4.5.3+wechatqrcode代码细节和 自编译类库(带下载地址) - 断舍离-重学JAVA之路 - 博客园 (cnblogs.com)
 */
@OptIn(MiraiInternalApi::class)
object WeChatQRCodeTool {
    //封装成了工具类，所以做成了单例




    fun decode(srcImage: ByteArray): MutableList<String>? {
        val image = Imgcodecs.imdecode(MatOfByte(*srcImage), Imgcodecs.IMREAD_UNCHANGED)
        //返回解析的字符串，如果图片有多个二维码，则会返回多个。
        val result2 = detector.detectAndDecode(image)
        return if (result2 != null && result2.size > 0) {
            result2
        } else null
    }


    fun decode(srcImage: File): MutableList<String>? {
        val image = Imgcodecs.imdecode(MatOfByte(*srcImage.readBytes()), Imgcodecs.IMREAD_UNCHANGED)
        //返回解析的字符串，如果图片有多个二维码，则会返回多个。
        val result2 = detector.detectAndDecode(image)
        return if (result2 != null && result2.size > 0) {
            result2
        } else null
    }


    @Volatile
    private lateinit var detector: WeChatQRCode


    /**
     * Mat转换为BufferedImage
     */
    fun matToBufferedImage(mat: Mat): BufferedImage? {
        if (mat.height() > 0 && mat.width() > 0) {
            val image = BufferedImage(
                mat.width(), mat.height(),
                BufferedImage.TYPE_3BYTE_BGR
            )
            val raster = image.raster
            val dataBuffer = raster.dataBuffer as DataBufferByte
            val data = dataBuffer.data
            mat[0, 0, data]
            return image
        }
        return null
    }

    /**
     * Mat转换为BufferedImage
     */
    fun mat2img(mat: Mat): BufferedImage {
        val dataSize = mat.cols() * mat.rows() * mat.elemSize().toInt()
        val data = ByteArray(dataSize)
        mat[0, 0, data]
        val type = if (mat.channels() == 1) 10 else 5
        if (type == 5) {
            var i = 0
            while (i < dataSize) {
                val blue = data[i + 0]
                data[i + 0] = data[i + 2]
                data[i + 2] = blue
                i += 3
            }
        }
        val image = BufferedImage(mat.cols(), mat.rows(), type)
        image.raster.setDataElements(0, 0, mat.cols(), mat.rows(), data)
        return image
    }

    init {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        //微信提供的4个模型配置文件放在resource/wechatqrcode文件夹里,
        //下载地址：WeChatCV/opencv_3rdparty: OpenCV - 3rdparty (github.com)
        val cl = this::class.java.classLoader
        val detectprototxt = Rika.dataFolder.resolve("resources/wechatqrcode/detect.prototxt").createParentFolder()
        val detectcaffemodel =  Rika.dataFolder.resolve("resources/wechatqrcode/detect.caffemodel").createParentFolder()
        val srprototxt =  Rika.dataFolder.resolve("resources/wechatqrcode/sr.prototxt").createParentFolder()
        val srcaffemodel =  Rika.dataFolder.resolve("resources/wechatqrcode/sr.caffemodel").createParentFolder()
        if(detectprototxt.exists().not()){
            detectprototxt.writeBytes(cl.getResourceAsStream("wechatqrcode/detect.prototxt")!!.readBytes())
        }
        if(detectcaffemodel.exists().not()){
            detectcaffemodel.writeBytes(cl.getResourceAsStream("wechatqrcode/detect.caffemodel")!!.readBytes())
        }
        if(srprototxt.exists().not()){
            srprototxt.writeBytes(cl.getResourceAsStream("wechatqrcode/sr.prototxt")!!.readBytes())
        }
        if(srcaffemodel.exists().not()){
            srcaffemodel.writeBytes(cl.getResourceAsStream("wechatqrcode/sr.caffemodel")!!.readBytes())
        }
        //实例化微信二维码扫描对象
        //如果打成jar，那么路径需要换到外部磁盘存储目录。
        detector = WeChatQRCode(
            detectprototxt.toString(),  //因为使用的getResource方法获取的是URL对象，而这个构造方法里需要传入File的路径，所以substring1去掉/D:/xx开头的/
            detectcaffemodel.toString(),
            srprototxt.toString(),
            srcaffemodel.toString()
        )
    }
}