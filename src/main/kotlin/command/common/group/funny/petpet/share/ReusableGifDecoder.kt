package xmmt.dituon.share

import com.madgag.gif.fmsware.GifDecoder
import java.io.BufferedInputStream

class ReusableGifDecoder : GifDecoder() {
    /**
     * 不会关闭BufferedInputStream, 便于复用
     */
    override fun read(`is`: BufferedInputStream): Int {
        init()
        if (`is` != null) {
            super.`in` = `is`
            readHeader()
            if (!err()) {
                readContents()
                if (super.frameCount < 0) {
                    super.status = STATUS_FORMAT_ERROR
                }
            }
        } else {
            super.status = STATUS_OPEN_ERROR
        }
        return super.status
    }

    public override fun err(): Boolean {
        return super.err()
    }
}