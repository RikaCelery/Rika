package org.celery.utils.strings

import java.math.BigDecimal
import java.math.RoundingMode


fun compare(proName1: String,proName2: String) : Double{
//    proName1 = "我是中国人"
//    proName2 = "我是中国湖南人"
    val number: Double
    try {
        number = getSimilarityRatio(proName1, proName2)
    } catch (e:Exception) {
        System.err.println(proName1 + " => " + proName2)
        return 101.0
    }
//    colorln("相似度:${number * 100} %").cyan()
//    println("  - $proName1")
//    println("  - $proName2")
    return number
}

/**
 * 分析比较 相似度
 * @param proName1
 * @param proName2
 * @return
 */
fun getSimilarityRatio(proName1: String, proName2: String): Double {
    val Length1 = proName1.length
    val Length2 = proName2.length
    var Distance = 0
    if (Length1 == 0) {
        Distance = Length2
    }
    if (Length2 == 0) {
        Distance = Length1
    }
    if (Length1 != 0 && Length2 != 0) {
        val Distance_Matrix = Array(Length1 + 1) { IntArray(Length2 + 1) }
        //编号
        var Bianhao = 0
        for (i in 0..Length1) {
            Distance_Matrix[i][0] = Bianhao
            Bianhao++
        }
        Bianhao = 0
        for (i in 0..Length2) {
            Distance_Matrix[0][i] = Bianhao
            Bianhao++
        }
        val Str_1_CharArray = proName1.toCharArray()
        val Str_2_CharArray = proName2.toCharArray()
        for (i in 1..Length1) {
            for (j in 1..Length2) {
                Distance = if (Str_1_CharArray[i - 1] == Str_2_CharArray[j - 1]) {
                    0
                } else {
                    1
                }
                val Temp1 = Distance_Matrix[i - 1][j] + 1
                val Temp2 = Distance_Matrix[i][j - 1] + 1
                val Temp3 = Distance_Matrix[i - 1][j - 1] + Distance
                Distance_Matrix[i][j] = if (Temp1 > Temp2) Temp2 else Temp1
                Distance_Matrix[i][j] =
                    if (Distance_Matrix[i][j] > Temp3) Temp3 else Distance_Matrix[i][j]
            }
        }
        Distance = Distance_Matrix[Length1][Length2]
    }
    val Aerfas = 1 - 1.0 * Distance / if (Length1 > Length2) Length1 else Length2
    val b = BigDecimal(Aerfas)
    return b.setScale(2,RoundingMode.HALF_DOWN).toDouble()
}
