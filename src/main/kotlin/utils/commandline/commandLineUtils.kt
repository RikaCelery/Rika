package com.celery.rika.utils.commandline

import com.celery.com.celery.rika.Rika
import java.io.File
import java.util.concurrent.TimeUnit

fun runCommand(
    cmd: List<String>,
    workDir: File = File("C:\\Users\\Celery\\Desktop"),
    timeoutAmount: Long = 60L,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): String? = runCatching {
    ProcessBuilder(cmd)
        .directory(workDir)
        .redirectErrorStream(true)
        .start().also { it.waitFor(timeoutAmount, timeUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()

fun String.runCommand(): Process {
    if (Rika.DEBUG_MODE) Rika.logger.debug(this)
    return Runtime.getRuntime().exec(this)
}

fun String.runCommandReadText(): String = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText()
