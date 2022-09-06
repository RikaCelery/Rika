package org.celery.utils.file

import org.celery.Rika
import org.apache.commons.codec.binary.Hex
import java.io.File
import java.security.MessageDigest

val ByteArray.md5: String
    get() {
        val mD5 = MessageDigest.getInstance("md5")
        mD5.update(this)
        return Hex.encodeHex(mD5.digest()).joinToString("")
    }
fun File.createParentFolder(): File {
    if (!parentFile.exists()) {
        parentFile.mkdirs()
    }
    return this
}

fun Rika.newTempFile(ext: String = "temp"): File {
    var file = resolveDataFile("temp/${System.currentTimeMillis()}.$ext")
    file.parentFile.apply { mkdirs() }
    var n = 1
    while (file.exists()) {
        file = resolveDataFile("temp/${System.currentTimeMillis() + n}.$ext")
        n++
    }
    return file
}