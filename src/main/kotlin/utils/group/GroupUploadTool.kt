package com.celery.rika.utils.group

import com.celery.com.celery.rika.Rika
import com.celery.rika.exceptions.DuplicateFileException
import com.celery.rika.exceptions.GroupSpaceFullException
import com.celery.rika.exceptions.SecurityCheckFailed
import com.celery.rika.utils.file.FileTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.file.AbsoluteFolder
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiLogger
import java.io.File
import java.io.IOException
import kotlin.io.path.Path

object GroupUploadTool {
    val logger = MiraiLogger.Factory.create(this::class)

    /**
     * 上传文件到群
     * @param group 群
     * @param file 文件
     * @param path 相对路径或文件名
     * @param parentFodder 父文件夹
     * @param allowDuplicate 是否允许重复
     * @param allowOverwrite 是否允许覆盖
     * @param autoDelete 是否自动删除上传成功的文件
     * @param autoHandleException 是否自动处理异常(发送到群内,对于系统错误会发聩到指定群)
     * @throws DuplicateFileException 如果文件已存在
     * @throws GroupSpaceFullException 如果群空间已满
     * @throws SecurityCheckFailed 如果安全检查失败
     */
    suspend fun uploadFile(
        group: Group,
        file: File,
        path: String = file.name,
        parentFodder: AbsoluteFolder? = null,
        allowDuplicate: Boolean = false,
        allowOverwrite: Boolean = false,
        autoHandleException: Boolean = true,
        autoDelete: Boolean = true
    ): Boolean {
        val mergedPath = parentFodder?.let { "${it}/$path" } ?: path
        return uploadFile0(group, file, mergedPath, allowDuplicate, allowOverwrite, autoHandleException, autoDelete)
    }

    private suspend fun uploadFile0(
        group: Group,
        file: File,
        path: String,
        allowDuplicate: Boolean = false,
        allowOverwrite: Boolean = false,
        autoHandleException: Boolean = true,
        autoDelete: Boolean = true
    ): Boolean {
        val externalResource = file.toExternalResource()
        if (allowDuplicate) {
            return uploadFileGroup(group, file, path, externalResource, autoHandleException, autoDelete)
        } else {
            group.files.root.children().toList().find {
                it.name == file.name && it.isFile
            }?.let {
                if (allowOverwrite) {
                    try {
                        if (it.delete()) {
                            return uploadFileGroup(group, file, path, externalResource, autoHandleException, autoDelete)
                        } else {
                            if (autoHandleException) {
                                group.sendMessage("上传失败，文件已存在${it.absolutePath},删除失败")
                                logger.error("文件已存在: $file,在: $group, 的: ${it.absolutePath},删除失败")
                                return false
                            }
                            throw DuplicateFileException("文件已存在: $file,在: $group, 的: ${it.absolutePath},删除失败")
                        }
                    } catch (ioException: IOException) {
                        if (autoHandleException) {
                            group.sendMessage("网络错误")
                            logger.error("网络错误${ioException.stackTraceToString()}")
                            return false
                        }
                    } catch (illegal: IllegalStateException) {
                        if (autoHandleException) {
                            group.sendMessage("内部错误")
                            logger.error("协议错误${illegal.stackTraceToString()}")
                            return false
                        }
                    } catch (permissonException: PermissionDeniedException) {
                        if (autoHandleException) {
                            group.sendMessage("权限不足,无法删除已存在的文件${it.absolutePath}")
                            logger.error("协议错误${permissonException.stackTraceToString()}")
                            return false
                        }
                    }
                }
                if (autoHandleException) {
                    group.sendMessage("上传失败，文件已存在${it.absolutePath}")
                    logger.error("文件已存在: $file,在: $group, 的: ${it.absolutePath}")
                    return false
                }
                throw DuplicateFileException("文件已存在: $file,在: $group, 的: ${it.absolutePath}")
            }
            return uploadFileGroup(group, file, path, externalResource, autoHandleException, autoDelete)
        }
    }

    private suspend fun uploadFileGroup(
        group: Group,
        file: File,
        path: String,
        externalResource: ExternalResource,
        autoHandleException: Boolean,
        autoDelete: Boolean
    ): Boolean {
        try {
            logger.debug("开始上传文件: $file, 到: $group, 的: $path")
            if (path.contains("/")) {
                val fodder = group.files.root.createFolder(path.substringBeforeLast("/"))
                fodder.uploadNewFile(path.substringAfterLast("/"), externalResource)
            } else
                group.files.uploadNewFile(path, externalResource)
            if (autoDelete) {
                logger.debug("开始删除文件: $file")
                file.delete()
            }
            logger.info("文件上传成功: $file,在: $group")
        } catch (e: PermissionDeniedException) {
            try {
                group.files.uploadNewFile(file.name, externalResource)
                if (autoDelete) {
                    logger.debug("开始删除文件: $file")
                    file.delete()
                }
                logger.info("文件上传成功: $file,在: $group 权限不足,未上传至指定路径")
            } catch (e: PermissionDeniedException) {
                if (autoHandleException) {
                    group.sendMessage("上传失败，权限不足")
                    logger.error("无法上传文件: $file,在: $group，请检查权限")
                    return false
                }
                throw e
            }
        } catch (e: Exception) {
            if (e.toString().contains("group space not enough") == true) {
                if (autoHandleException) {
                    group.sendMessage("上传失败，群空间不足")
                    logger.error("群空间不足，无法上传文件: $file,在: $group")
                    return false
                }
                throw GroupSpaceFullException("群空间不足，无法上传文件: $file,在: $group")
            } else if (e.message?.contains(Regex("(security)?(check failed)")) == true) {
                if (autoHandleException) {
                    logger.info("文件安全检查失败: $file,在: $group, 自动加密中...")
                    val newFile = FileTools.getArchive(
                        Rika.resolveDataFile("temp/${file.name.hashCode()}.${file.extension}"),
                        listOf(file), Path(path).fileName.toString()
                    )
                    val newExr = newFile.toExternalResource()
                    try {
                        group.files.uploadNewFile(path, newExr)
                        logger.info("文件上传成功: $file,在: $group 密码: ${Path(path).fileName}")
                    } catch (e: Exception) {
                        group.sendMessage("无法上传文件: $file")
//                        ErrorReport.report("无法上传文件: $file,在: $group, $e")
                        logger.error("无法上传文件: $file,在: $group，$e")
                        return false
                    } finally {
                        withContext(Dispatchers.IO) {
                            newExr.close()
                        }
                    }
                } else
                    throw SecurityCheckFailed("文件安全检查失败: $file,在: $group")
            }
        } finally {
            externalResource.close()

        }
        logger.debug("成功上传文件: $file, 到: $group, 的: $path")
        return true
    }


}