package org.celery.utils.number

import net.mamoe.mirai.utils.debug
import org.celery.Rika
import kotlin.random.Random

private val random = Random(System.currentTimeMillis())

@Synchronized
fun getRandomer(seed:Long) = Random(seed)

fun probability(double: Double): Boolean {
    return random.nextFloat().also { Rika.logger.debug { "rand$it" } }<=double
}

/**
 * 获取下一个在 0（包括）和 1（不包括）之间均匀分布的随机Float值。
 */
fun randomFloat() = random.nextFloat()
/**
 * 获取下一个在 [from]（包括）和 [until]（不包括）之间均匀分布的随机Int值。
 */
fun randomInt(from:Int,until:Int): Int {
    return random.nextInt(from,until)
}