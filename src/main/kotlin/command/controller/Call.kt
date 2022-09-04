package org.celery.command.controller

@kotlinx.serialization.Serializable
/**
 * 代表某个用户执行一次命令
 * @param commandId 指令的唯一id 以.分隔父子指令
 * @param timeStamp ms
 */
data class Call(
    val commandId: String,
    val userId: Long?,
    val subjectId: Long?,
    val timeStamp: Long = System.currentTimeMillis()
)