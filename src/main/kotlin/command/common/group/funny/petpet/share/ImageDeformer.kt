package xmmt.dituon.share

import java.awt.Polygon
import java.awt.geom.Point2D
import java.awt.image.BufferedImage

// TODO 支持图片变形
// 应该用3D库进行3D运算...伪3D效率比较低... 欢迎Pr!
// From https://math.stackexchange.com/questions/296794
object ImageDeformer {
    private fun min(v1: Int, v2: Int, v3: Int, v4: Int): Int {
        return Math.min(Math.min(v1, v2), Math.min(v3, v4))
    }

    private fun max(v1: Int, v2: Int, v3: Int, v4: Int): Int {
        return Math.max(Math.max(v1, v2), Math.max(v3, v4))
    }

    fun computeImage(image: BufferedImage, point: Array<Point2D?>): BufferedImage {
        val w = image.width
        val h = image.height
        val ip0: Point2D = Point2D.Double(0.0, 0.0)
        val ip1: Point2D = Point2D.Double(0.0, h.toDouble())
        val ip2: Point2D = Point2D.Double(w.toDouble(), h.toDouble())
        val ip3: Point2D = Point2D.Double(w.toDouble(), 0.0)
        val originToDeformed =
            computeProjectionMatrix(arrayOf(point[0], point[1], point[2], point[3]), arrayOf(ip0, ip1, ip2, ip3))
        val deformedToOrigin = Matrix3D(originToDeformed)
        deformedToOrigin.invert()
        val deformedip0: Point2D = Point2D.Double(0.0, 0.0)
        val deformedip1: Point2D = Point2D.Double(0.0, h.toDouble())
        val deformedip2: Point2D = Point2D.Double(w.toDouble(), h.toDouble())
        val deformedip3: Point2D = Point2D.Double(w.toDouble(), 0.0)
        originToDeformed.transform(deformedip0)
        originToDeformed.transform(deformedip1)
        originToDeformed.transform(deformedip2)
        originToDeformed.transform(deformedip3)
        val deformedArea = Polygon()
        deformedArea.addPoint(deformedip0.x.toInt(), deformedip0.y.toInt())
        deformedArea.addPoint(deformedip1.x.toInt(), deformedip1.y.toInt())
        deformedArea.addPoint(deformedip2.x.toInt(), deformedip2.y.toInt())
        deformedArea.addPoint(deformedip3.x.toInt(), deformedip3.y.toInt())
        val deformedWidth = deformedArea.bounds.width
        val deformedHeight = deformedArea.bounds.bounds.height
        val result = BufferedImage(deformedWidth, deformedHeight, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until deformedHeight) {
            for (x in 0 until deformedWidth) {
                if (deformedArea.contains(x, y)) {
                    val originPoint: Point2D = Point2D.Double(x.toDouble(), y.toDouble())
                    deformedToOrigin.transform(originPoint)
                    val originX = Math.min(Math.round(originPoint.x).toInt(), w - 1)
                    val originY = Math.min(Math.round(originPoint.y).toInt(), h - 1)
                    val rgb = image.getRGB(originX, Math.max(originY, 0))
                    result.setRGB(x, y, rgb)
                }
            }
        }
        return result
    }

    private fun computeProjectionMatrix(p0: Array<Point2D?>, p1: Array<Point2D?>): Matrix3D {
        val m0 = computeProjectionMatrix(p0)
        val m1 = computeProjectionMatrix(p1)
        m1.invert()
        m0.mul(m1)
        return m0
    }

    private fun computeProjectionMatrix(p: Array<Point2D?>): Matrix3D {
        val m = Matrix3D(
            p[0]!!.x, p[1]!!.x, p[2]!!.x,
            p[0]!!.y, p[1]!!.y, p[2]!!.y,
            1.0, 1.0, 1.0
        )
        val p3 = Point3D(p[3]!!.x, p[3]!!.y, 1.0)
        val mInv = Matrix3D(m)
        mInv.invert()
        mInv.transform(p3)
        m.m00 *= p3.x
        m.m01 *= p3.y
        m.m02 *= p3.z
        m.m10 *= p3.x
        m.m11 *= p3.y
        m.m12 *= p3.z
        m.m20 *= p3.x
        m.m21 *= p3.y
        m.m22 *= p3.z
        return m
    }

    private class Point3D internal constructor(var x: Double, var y: Double, var z: Double)
    private class Matrix3D {
        var m00: Double
        var m01: Double
        var m02: Double
        var m10: Double
        var m11: Double
        var m12: Double
        var m20: Double
        var m21: Double
        var m22: Double

        internal constructor(
            m00: Double, m01: Double, m02: Double,
            m10: Double, m11: Double, m12: Double,
            m20: Double, m21: Double, m22: Double
        ) {
            this.m00 = m00
            this.m01 = m01
            this.m02 = m02
            this.m10 = m10
            this.m11 = m11
            this.m12 = m12
            this.m20 = m20
            this.m21 = m21
            this.m22 = m22
        }

        internal constructor(m: Matrix3D) {
            m00 = m.m00
            m01 = m.m01
            m02 = m.m02
            m10 = m.m10
            m11 = m.m11
            m12 = m.m12
            m20 = m.m20
            m21 = m.m21
            m22 = m.m22
        }

        fun invert() {
            val invDet = 1.0 / determinant()
            val nm00 = m22 * m11 - m21 * m12
            val nm01 = -(m22 * m01 - m21 * m02)
            val nm02 = m12 * m01 - m11 * m02
            val nm10 = -(m22 * m10 - m20 * m12)
            val nm11 = m22 * m00 - m20 * m02
            val nm12 = -(m12 * m00 - m10 * m02)
            val nm20 = m21 * m10 - m20 * m11
            val nm21 = -(m21 * m00 - m20 * m01)
            val nm22 = m11 * m00 - m10 * m01
            m00 = nm00 * invDet
            m01 = nm01 * invDet
            m02 = nm02 * invDet
            m10 = nm10 * invDet
            m11 = nm11 * invDet
            m12 = nm12 * invDet
            m20 = nm20 * invDet
            m21 = nm21 * invDet
            m22 = nm22 * invDet
        }

        // From http://www.dr-lex.be/random/matrix_inv.html
        fun determinant(): Double {
            return m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20)
        }

        fun mul(factor: Double) {
            m00 *= factor
            m01 *= factor
            m02 *= factor
            m10 *= factor
            m11 *= factor
            m12 *= factor
            m20 *= factor
            m21 *= factor
            m22 *= factor
        }

        fun transform(p: Point3D) {
            val x = m00 * p.x + m01 * p.y + m02 * p.z
            val y = m10 * p.x + m11 * p.y + m12 * p.z
            val z = m20 * p.x + m21 * p.y + m22 * p.z
            p.x = x
            p.y = y
            p.z = z
        }

        fun transform(pp: Point2D) {
            val p = Point3D(pp.x, pp.y, 1.0)
            transform(p)
            pp.setLocation(p.x / p.z, p.y / p.z)
        }

        fun mul(m: Matrix3D) {
            val nm00 = m00 * m.m00 + m01 * m.m10 + m02 * m.m20
            val nm01 = m00 * m.m01 + m01 * m.m11 + m02 * m.m21
            val nm02 = m00 * m.m02 + m01 * m.m12 + m02 * m.m22
            val nm10 = m10 * m.m00 + m11 * m.m10 + m12 * m.m20
            val nm11 = m10 * m.m01 + m11 * m.m11 + m12 * m.m21
            val nm12 = m10 * m.m02 + m11 * m.m12 + m12 * m.m22
            val nm20 = m20 * m.m00 + m21 * m.m10 + m22 * m.m20
            val nm21 = m20 * m.m01 + m21 * m.m11 + m22 * m.m21
            val nm22 = m20 * m.m02 + m21 * m.m12 + m22 * m.m22
            m00 = nm00
            m01 = nm01
            m02 = nm02
            m10 = nm10
            m11 = nm11
            m12 = nm12
            m20 = nm20
            m21 = nm21
            m22 = nm22
        }
    }
}