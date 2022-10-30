package org.celery.command.common.hutao

import org.celery.utils.number.randomInt
import java.io.File

fun main() {
    File("胡桃.txt").apply {
        if (exists().not()) createNewFile()
    }.readText().split("[func_start]").map(String::trimIndent).filterNot(String::isBlank).map(String::lines).forEach {
        val regex = it.first()
        if (it[1] == "RandomReply") {
            println("------------------")
            var sum = 0
            println("probility: ${it[2]}")
            val map = it.drop(3).filterNot(String::isBlank).map { s ->
                val start = sum
                if (s.first().isDigit().not()) {
                    sum+=1
                    return@map Triple(1, start until  sum, s)
                }
                val weight = s.substringBefore('|').toInt()
                val content = (s.substringAfter('|'))
                sum += weight
                Triple(weight, start until  sum, content)
            }.onEach {
//                println(it)
            }
            println(map.sumOf { it.second.last+1 - it.second.first })
            println(sum)
            val randomInt = randomInt(0, sum)
            map.find { randomInt in it.second }.let {
                println(it?.third)
            }

        } else {
//            val replys = it.drop(1)
//            println("regex = ${regex}")
//            println("replys = ${replys}")
        }
    }
    println("jjjjjjjjj")
}