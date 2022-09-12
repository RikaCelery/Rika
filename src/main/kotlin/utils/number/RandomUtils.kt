package org.celery.utils.number

import kotlin.random.Random

private val random = Random(System.currentTimeMillis())

fun probability(double: Double): Boolean {
    return random.nextFloat()<=double
}

/**
 * 获取下一个在 0（包括）和 1（不包括）之间均匀分布的随机Float值。
 */
fun randomFloat() = random.nextFloat()