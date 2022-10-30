package xmmt.dituon.share

import command.common.group.funny.petpet.share.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.regex.Pattern

class TextModel(textData: TextData, extraInfo: TextExtraData?) {
    protected var text: String
    protected var pos = intArrayOf(2, 14)

    /**
     * 获取颜色 (默认为 #191919)
     */
    var color = Color(25, 25, 25, 255) // #191919
        protected set
    protected var font: Font
    protected var align: TextAlign?
    protected var wrap: TextWrap?
    var position: List<Position>?
        protected set
    private var line: Short = 1

    init {
        text = if (extraInfo != null) buildText(textData.text, extraInfo) else textData.text.replace("\"", "")
        pos = if (textData.pos != null) setPos(textData.pos) else pos
        color = if (textData.color != null) BasePetService.Companion.decodeColor(
            textData.color,
            shortArrayOf(25, 25, 25, 255)
        ) else color
        font = loadFont(
            if (textData.font != null) textData.font.replace("\"", "") else "黑体",
            textData.size ?: 12,
            textData.style ?: TextStyle.PLAIN
        )
        align = textData.align
        wrap = textData.wrap
        position = textData.position
        if (position == null || position!!.size != 2) position = null
    }

    private fun buildText(text: String, extraData: TextExtraData): String {
        var text = text
        text = text.replace("\"", "")
            .replace("\$from", extraData.fromReplacement)
            .replace("\$to", extraData.toReplacement)
            .replace("\$group", extraData.groupReplacement)
            .replace("\\n", "\n")
        val regex = "\\\$txt([1-9])\\[(.*)]" //$txt(num)[(xxx)]
        val m = Pattern.compile(regex).matcher(text)
        while (m.find()) {
            val i = m.group(1).toShort()
            val replaceText = if (i > extraData.textList.size) m.group(2) else extraData.textList[i - 1]
                .replace("\\n", "\n").replace("\\s", " ")
            text = text.replace(m.group(0), replaceText)
        }
        val chars = text.toCharArray()
        for (t in chars) {
            if (t == '\n' && chars[chars.size - 1] != '\n') {
                line++
            }
        }
        return text
    }

    private fun setPos(posElements: List<Int>?): IntArray {
        val x = posElements!![0]
        val y = posElements[1]
        val w = if (posElements.size == 3) posElements[2] else 200
        return intArrayOf(x, y, w)
    }

    /**
     * 获取构建后的文本数据
     */
    @JvmName("getText1")
    fun getText(): String {
        if (wrap === TextWrap.BREAK && pos.size >= 3) {
            val width = getWidth(font)
            if (width <= pos[2]) return text
            val lineAp = (width / pos[2]).toShort()
            val builder = StringBuilder(text)
            val lineWidth = (text.length / lineAp).toShort()
            var i: Short = 1
            while (i <= lineAp) {
                builder.insert(lineWidth * i++, '\n')
            }
            return builder.toString()
        }
        return text
    }

    /**
     * 获取构建后的坐标(深拷贝)
     *
     * @return int[2]{x, y}
     */
    @JvmName("getPos1")
    fun getPos(): IntArray {
        when (align) {
            TextAlign.CENTER -> return intArrayOf(
                pos[0] - getWidth(getFont()) / 2,
                pos[1] + getHeight(getFont()) / 2
            )
            TextAlign.RIGHT -> return intArrayOf(pos[0] - getWidth(getFont()), pos[1])
        }
        return pos.clone()
    }

    fun zoomFont(multiple: Float) {
        font = Font(font.fontName, font.style, Math.round(font.size * multiple))
    }

    /**
     * 获取构建后的字体
     */
    @JvmName("getFont1")
    fun getFont(): Font {
        if (wrap === TextWrap.ZOOM) {
            val multiple = Math.min(1.0f, pos[2].toFloat() / getWidth(font))
            return Font(font.fontName, font.style, Math.round(font.size * multiple))
        }
        return font
    }

    /**
     * 在Graphics2D对象上绘制TextModel
     *
     * @param g2d           画布
     * @param stickerWidth  画布宽度, 用于计算坐标
     * @param stickerHeight 画布高度, 用于计算坐标
     */
    fun drawAsG2d(g2d: Graphics2D, stickerWidth: Int, stickerHeight: Int) {
        if (position == null) {
            ImageSynthesisCore.Companion.g2dDrawText(g2d, getText(), getPos(), color, getFont())
            return
        }
        val pos = getPos()
        when (position!![0]) {
            Position.RIGHT -> pos[0] = stickerWidth - pos[0]
            Position.CENTER -> pos[0] = stickerWidth / 2 + pos[0]
        }
        when (position!![1]) {
            Position.BOTTOM -> pos[1] = stickerHeight - pos[1]
            Position.CENTER -> pos[1] = stickerHeight / 2 + pos[1]
        }
        ImageSynthesisCore.Companion.g2dDrawText(g2d, getText(), pos, color, getFont())
    }

    /**
     * 获取文字渲染后的宽度 (包含 \n)
     *
     * @param font 渲染字体
     */
    fun getWidth(font: Font?): Int {
        var width = 0
        for (p in text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            width = Math.max(width, getTextWidth(p, font))
        }
        return width
    }

    /**
     * 获取字体渲染后的高度 (包含 \n)
     *
     * @param font 渲染字体
     */
    fun getHeight(font: Font): Int {
        return getTextHeight(text, font) * line
    }

    companion object {
        private var container: Graphics2D? = null
        private fun loadFont(fontName: String, size: Int, style: TextStyle?): Font {
            return Font(fontName, style!!.ordinal, size)
        }

        /**
         * 获取文字渲染后的宽度 (不渲染 \n)
         *
         * @param font 渲染字体
         */
        fun getTextWidth(text: String?, font: Font?): Int {
            if (container == null) container = BufferedImage(1, 1, 1).createGraphics()
            return container!!.getFontMetrics(font).stringWidth(text)
        }

        /**
         * 获取字体渲染后的高度 (不渲染 \n)
         *
         * @param font 渲染字体
         */
        fun getTextHeight(text: String?, font: Font): Int {
            if (container == null) container = BufferedImage(1, 1, 1).createGraphics()
            return font.createGlyphVector(
                container!!.getFontMetrics(font).fontRenderContext, text
            ).visualBounds.height.toInt()
        }
    }
}