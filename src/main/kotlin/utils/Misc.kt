package org.celery.utils

inline fun <R : Any>withRetry(maxRetry:Int=5, block:()->R): R {
    var exception:Exception?=null
    for (n in 0..maxRetry){
        try {
            return block()
        } catch (e:Exception) {
            if (exception==null)
                exception = e
            exception.addSuppressed(e)
        }
    }
    throw exception!!
}