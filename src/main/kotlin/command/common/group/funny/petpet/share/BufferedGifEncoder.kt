package command.common.group.funny.petpet.share

import java.awt.image.RenderedImage
import java.io.*
import javax.imageio.*
import javax.imageio.metadata.IIOInvalidTreeException
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream

class BufferedGifEncoder(imageType: Int, delay: Int, loop: Boolean) {
    protected var writer: ImageWriter
    protected var params: ImageWriteParam
    protected var metadata: IIOMetadata
    protected var image: ImageOutputStream
    var output: InputStream? = null
        protected set

    init {
        writer = ImageIO.getImageWritersBySuffix("gif").next()
        params = writer.defaultWriteParam
        val imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType)
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params)
        configureRootMetadata(delay, loop)
        val os = ByteArrayOutputStream()
        image = ImageIO.createImageOutputStream(os)
        writer.output = image
        writer.prepareWriteSequence(null)
    }

    fun getBytes(ios: ImageOutputStream): ByteArray {
        val bos = ByteArrayOutputStream(255)
        try {
            ios.seek(0)
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        while (true) {
            try {
                bos.write(ios.readByte().toInt())
            } catch (e: EOFException) {
                break
            } catch (e: IOException) {
                println("Error processing the Image Stream")
                break
            }
        }
        return bos.toByteArray()
    }

    @Throws(IIOInvalidTreeException::class)
    private fun configureRootMetadata(delay: Int, loop: Boolean) {
        val metaFormatName = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(metaFormatName) as IIOMetadataNode
        val graphicsControlExtensionNode = getNode(root, "GraphicControlExtension")
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10))
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0")
        val appExtensionsNode = getNode(root, "ApplicationExtensions")
        val child = IIOMetadataNode("ApplicationExtension")
        child.setAttribute("applicationID", "NETSCAPE")
        child.setAttribute("authenticationCode", "2.0")
        val loopContinuously = if (loop) 0 else 1
        child.userObject = byteArrayOf(0x1, (loopContinuously and 0xFF).toByte(), 0.toByte())
        appExtensionsNode.appendChild(child)
        metadata.setFromTree(metaFormatName, root)
    }

    @Throws(IOException::class)
    fun addFrame(img: RenderedImage?) {
        writer.writeToSequence(IIOImage(img, null, metadata), params)
    }

    @Throws(IOException::class)
    fun finish() {
        writer.endWriteSequence()
        output = ByteArrayInputStream(getBytes(image))
    }

    companion object {
        private fun getNode(rootNode: IIOMetadataNode, nodeName: String): IIOMetadataNode {
            val nNodes = rootNode.length
            for (i in 0 until nNodes) {
                if (rootNode.item(i).nodeName.equals(nodeName, ignoreCase = true)) {
                    return rootNode.item(i) as IIOMetadataNode
                }
            }
            val node = IIOMetadataNode(nodeName)
            rootNode.appendChild(node)
            return node
        }
    }
}