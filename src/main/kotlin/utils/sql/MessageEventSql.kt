package org.celery.utils.sql

import org.celery.utils.file.md5
import org.celery.utils.group.truncate
import org.celery.utils.http.HttpUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.config.main.MainConfig
import org.celery.config.main.SqlConfig
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.*


object MessageEventSql {
    val logger = MiraiLogger.Factory.create(this.javaClass)
    var JDBC_DRIVER = "com.mysql.cj.jdbc.Driver"
    var DB_URL = SqlConfig.eventsUrl

    // 数据库的用户名与密码，需要根据自己的设置
    var USER = SqlConfig.eventsName
    var PASS = SqlConfig.eventsPass
    var conn: Connection? = null

    //初始化
    init {
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER)
            // 打开链接
            logger.debug("EventsSql: $USER 连接数据库qqmessage2...")
            conn = DriverManager.getConnection(DB_URL, USER, PASS)
        } catch (e: Exception) {
            logger.warning("EventsSql: 连接数据库失败")
        }
    }

    private suspend fun GroupMessageEvent.addmessageChain(
        messageChain: MessageChain,
        Stringmessage: String,
        forward: Boolean = false
    ) {
        if (messageChain.any { it.toString().contains(Regex("晚间好物|(恰 饭 时 间 到)")) }) {
            val file = Rika.dataFolder.resolve("MessageDataBase\\Filtered Message.txt")
            file.appendText(messageChain.serializeToJsonString() + "\n")
            logger.debug("忽略了一条被匹配为广告的消息,消息已被保存至${file.absolutePath}")
            return
        }
        val noSource = Regex("^\\[mirai:source:\\[\\d+],\\[-?\\d+]]")
        val hash = this.message.hashCode().toString()
        fun saveMessage() {
            val sql = "INSERT INTO qqmessage2.groupmessagesevent " +
                    "(sendTime, `group`, senderName, sender, permission, message, jsonStr, hashcode,forwarder)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
            try {
                conn!!.prepareStatement(sql).also {
                    it.setObject(1, this.message.source.time)
                    it.setObject(2, this.group.id)
                    it.setObject(3, this.senderName)
                    it.setObject(4, this.sender.id)
                    it.setObject(5, this.permission.toString())
                    it.setObject(6, this.message.toString().replace(noSource, ""))
                    it.setObject(7, this.message.serializeToJsonString())
                    it.setObject(8, hash)
                    it.setObject(9, "notForward")
                }.execute()
            } catch (e: SQLIntegrityConstraintViolationException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }

        //save contents in this messageChain
//        if (RecoderConfig.image)
        messageChain.filterIsInstance<Image>().forEach { image: Image ->
            try {
                val sql =
                    "INSERT INTO qqmessage2.imagemap (imageMd5,imageId, imagePath,imageType,imageIdRaw,hashcode) VALUES (?, ?, ?, ?, ?, ?);"
                val imageBytes = HttpUtils.downloader(image.queryUrl())
                //launch QrTask
                GlobalScope.launch {
                    try {
                        val fronId = try {
                            messageChain.source.fromId
                        } catch (e: Exception) {
                            0
                        }
//                            readQr(imageBytes, group,image,fronId)?.let {
//                                if (QrScan.ignoreContent.any { regex -> Regex(regex).matches(it.content) }||it.content.contains("http://papamiao.xyz"))
//                                    return@let
//                                logger.info("Qr: ${it.toString().replace("\n","")}")
//                                Rika.dataFolder.resolve("QrCode").apply { if (!exists()) createNewFile() }.writeText(it.toString().replace("\n","")+"\n")
//                                Bot.getInstance(BOT_ID).getFriend(1436898372)?.sendMessage("有新的二维码")
//                            }
                    } catch (e: Exception) {
//                            logger.error(e)
                    }
                }
                val ext = when (imageBytes[0].toInt()) {
                    -1 -> ".jpg"
                    -119 -> ".png"
                    71 -> ".gif"
                    69 -> ".bmp"
                    else -> ".mirai"
                }
                val file =
                    Rika.resolveDataFile("MessageDataBase\\Image\\${if (image.isEmoji) "Emoji" else "Photo"}\\${imageBytes.md5 + ext}")
                        .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }
                if (!file.exists()) {
//                        if (Rika.DEBUG_MODE)
                    logger.info((if (image.isEmoji) "emoji" else "photo") + " saved ${image.imageId}")
                    file.writeBytes(imageBytes)
                }
//                    else if (Rika.DEBUG_MODE)
//                        loggerBack.trace((if(image.isEmoji) "emoji" else "photo")+" Existed ${file.absolutePath}")
                conn!!.prepareStatement(sql).also {
                    it.setObject(1, imageBytes.md5)
                    it.setObject(2, image.imageId.replace(Regex("\\.\\w+"), ext))
                    it.setObject(3, file.absolutePath)
                    it.setObject(4, if (image.isEmoji) "emoji" else "photo")
                    it.setObject(5, image.imageId)
                    it.setObject(6, hash)
                }.execute()
                delay(200)
            } catch (e: SQLIntegrityConstraintViolationException) {
                //do nothing
            } catch (e: Exception) {
                Rika.dataFolder.resolve("log\\Msglog\\error.log").writeText(messageChain.serializeToJsonString())
                logger.error(image.imageId + "\n\t" + e.stackTraceToString())
            }
        }

//        if (RecoderConfig.flashImage)
        messageChain.filterIsInstance<FlashImage>().forEach { flashImage: FlashImage ->
            try {
                val sql =
                    "INSERT INTO qqmessage2.imagemap (imageMd5,imageId, imagePath,imageIdRaw, hashcode,imageType)" +
                            "VALUES (?, ?, ?, ?, ?,?);"
                val image = flashImage.image
                val imageBytes = HttpUtils.downloader(image.queryUrl())

//                    GlobalScope.launch {
//                        try {
//                            val fronId = try {
//                                messageChain.source.fromId
//                            } catch (e: Exception) {
//                                0
//                            }
//                            readQr(imageBytes, group, image, fronId)?.let {
//                                if (it.content.contains("http://papamiao.xyz"))
//                                    return@let
//                                logger.info("Qr: $it")
////                                Bot.getInstance(BOT_ID).getFriend(1436898372)?.sendMessage(it)
//                            }
//                        } catch (e: Exception) {
//                            logger.error(e)
//                        }
//                    }
                val ext = when (imageBytes[0].toInt()) {
                    -1 -> ".jpg"
                    -119 -> ".png"
                    71 -> ".gif"
                    69 -> ".bmp"
                    else -> ".mirai"
                }
                val file = Rika.resolveDataFile("MessageDataBase\\Image\\FlashPhoto\\${imageBytes.md5 + ext}")
                    .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }

                if (!file.exists()) {
//                        if (Rika.DEBUG_MODE)
                    logger.info("flash saved ${image.imageId}")
                    file.writeBytes(imageBytes)
                }
//                    }else if (Rika.DEBUG_MODE)
//                        logger.info("flash existed ${image.imageId}")
                try {
                    conn!!.prepareStatement(sql).also {
                        it.setObject(1, imageBytes.md5)
                        it.setObject(2, image.imageId.replace(Regex("\\.\\w+"), ext))
                        it.setObject(3, file.absolutePath)
                        it.setObject(4, image.imageId)
                        it.setObject(5, hash)
                        it.setObject(6, "flash")
                    }.execute()
                } catch (e: SQLIntegrityConstraintViolationException) {
                    //do nothing
                } catch (e: Exception) {
                    Rika.resolveDataFile("")
                    logger.error(image.imageId)
                    logger.error(e.toString())
                }
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
        }

//        if (RecoderConfig.audio)
        messageChain.filterIsInstance<OnlineAudio>().forEach { onlineAudio: OnlineAudio ->
            try {
                val bytes = HttpUtils.downloader(onlineAudio.urlForDownload)
                val ext = Regex("\\.\\w+").findAll(onlineAudio.filename).last().value

                try {
                    val sql =
                        "INSERT INTO qqmessage2.audiomap(fileName, fileMd5, length, filePath, fileSize,hashcode)" +
                                "VALUES (?, ?, ?, ?, ?, ?);"
                    try {
                        val file = Rika.resolveDataFile("MessageDataBase\\Audio\\${bytes.md5 + ext}")
                            .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }
                        file.writeBytes(bytes)
                        conn!!.prepareStatement(sql).also {
                            it.setObject(1, onlineAudio.filename)
                            it.setObject(2, onlineAudio.fileMd5.md5)
                            it.setObject(3, onlineAudio.length)
                            it.setObject(4, file.absolutePath)
                            it.setObject(5, onlineAudio.fileSize)
                            it.setObject(6, hash)
                        }.execute()
                        logger.info("audiosaved ${onlineAudio.filename}")
                    } catch (_: SQLIntegrityConstraintViolationException) {
                    }
                } catch (e: Exception) {
                    logger.error(e.toString())
                }
                delay(300)
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
        }

//        if (RecoderConfig.file)
        messageChain.filterIsInstance<FileMessage>().forEach { fileMessage: FileMessage ->
            try {
                val url = fileMessage.toAbsoluteFile(group)!!.getUrl()
                if (sender.id != 1436898372L) {
                    val fileTmp = url?.let { HttpUtils.downloadToFile(it) }
                    fileTmp?.let {
                        val ext = fileMessage.name.substringAfterLast('.', "none")
                        val file = Rika.resolveDataFile("MessageDataBase\\File\\${it.nameWithoutExtension + '.' + ext}")
                            .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }
                        Files.move(it.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        try {
                            val sql = "INSERT INTO qqmessage2.filemap (fileName,  filePath, fileSize,Md5, hashcode)" +
                                    "VALUES (?, ?, ?, ?, ?);"
                            try {
                                conn!!.prepareStatement(sql).also {
                                    it.setObject(1, fileMessage.name)
                                    it.setObject(2, file.absolutePath)
                                    it.setObject(3, fileMessage.size)
                                    it.setObject(4, file.nameWithoutExtension)
                                    it.setObject(5, hash)
                                }.execute()
                                logger.info("file saved in sql ${fileMessage.name}")
                                Stringmessage.replace(
                                    fileMessage.name,
                                    "${fileMessage.name}(${file.nameWithoutExtension})"
                                )
                            } catch (_: SQLIntegrityConstraintViolationException) {
                            }
                        } catch (e: Exception) {
                            logger.error(e.toString())
                            e.printStackTrace()
                        }
                    } ?: logger.warning(fileMessage.name + "\t文件下载失败,尝试转发").also {
                        Bot.instances[0].getFriend(MainConfig.botOwner)?.let {
                            fileMessage.sendTo(Bot.instances[0].getFriend(MainConfig.botOwner)!!)
                        } ?: logger.error("消息转发失败,日志已记录").also {
                            Rika.resolveDataFile("errorLog/ErrorLog.txt")
                        }
                    }
                    delay(300)
                }
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
        }

//        if (RecoderConfig.forward)
        messageChain.filterIsInstance<ForwardMessage>().forEach { forwardMessage ->
            logger.info("处理转发消息${forwardMessage.nodeList.size}条")
            forwardMessage.nodeList.forEach { node: ForwardMessage.Node ->
                //if (node.messageChain .equals(PlainText))
                addForwardToSql(
                    messageChain = node.messageChain,  //转发消息的内容
                    senderId = node.senderId,  //转发消息内部消息发送者
                    senderName = node.senderName,  //
                    senderPermission = "NULL", //
                    time = try {
                        messageChain.time
                    } catch (e: NoSuchElementException) {
                        0
                    }, //这条消息被转发到这里的时间
                    groupId = try {
                        messageChain.source.targetId
                    } catch (e: NoSuchElementException) {
                        0L
                    }, //这条转发消息发送者所在群
                    forwarder = try {
                        messageChain.source.fromId
                    } catch (e: NoSuchElementException) {
                        0L
                    }, //这条转发消息的发送者
                    hash = hash
                )
                try {
                    this.addmessageChain(node.messageChain, Stringmessage, forward = true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(300)
            }
            logger.info("转发消息${forwardMessage.nodeList.size}处理完成")
        }
        //save this messageChain
        if (!forward) //转发的有单独处理
            saveMessage()
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun addmessage(groupMessageEvent: GroupMessageEvent, time: Int) {
        if (conn == null) {
            return
        }
        val str = groupMessageEvent.message.toString()
        var showDelay = false
        if (groupMessageEvent.message.any { it is Image } || groupMessageEvent.message.any { it is ForwardMessage } || groupMessageEvent.message.any { it is FileMessage })
            showDelay = true
        GlobalScope.launch {
            val startTime = System.currentTimeMillis()
            val id = groupMessageEvent.message.serializeToMiraiCode().truncate(20)
            groupMessageEvent.addmessageChain(groupMessageEvent.message, str)
            val delayTime = System.currentTimeMillis() - startTime
            if (showDelay && delayTime > 800)
                println("记录群消息完成: $id 耗时: ${delayTime}ms")
        }
//        println("add to GlobalScope ${groupMessageEvent.message.content}")
    }

    fun getMessages(subgect: Long, member: Long): MutableList<MessageSearchResult> {
        if (conn == null) {
            return mutableListOf()
        }
        if (Rika.DEBUG_MODE) logger.debug("EventsSql.getMessages() 实例化Statement对象...")
        val stmt: Statement? = conn?.createStatement()
        try {
            // 执行查询
            val sql =
                "SELECT sendTime, jsonStr FROM qqmessage2.groupmessagesevent where `group`=${subgect} and sender=${member} "
            val rs = stmt?.executeQuery(sql)
            // 展开结果集数据库
            val list = mutableListOf<MessageSearchResult>()
            if (rs != null) {
                while (rs.next()) {
                    // 通过字段检索
                    var str = MessageSearchResult(rs.getInt("sendTime"), rs.getString("jsonStr"))
                    list.add(str)
                }
            }
            // 完成后关闭
            rs?.close()
            stmt?.close()
            return list
        } catch (se: SQLException) {
            // 处理 JDBC 错误
            se.printStackTrace()
        } catch (e: Exception) {
            // 处理 Class.forName 错误
            e.printStackTrace()
        } finally {
            // 关闭资源
            try {
                stmt?.close()
            } catch (_: SQLException) {
            } // 什么都不做
        }
        return mutableListOf()
    }

    private fun addForwardToSql(
        messageChain: MessageChain,
        groupId: Long,
        senderId: Long,
        senderName: String,
        senderPermission: String,
        time: Int,
        forwarder: Long,
        hash: String
    ) {
        var sql = "INSERT INTO qqmessage2.groupmessagesevent " +
                "(sendTime, `group`, senderName, sender, permission, message, jsonStr,forwarder,hashcode)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
        try {
            conn!!.prepareStatement(sql).also {
                try {
                    it.setObject(1, time)
                } catch (e: NoSuchElementException) {
                    it.setObject(1, 0)
                }
                it.setObject(2, groupId)
                it.setObject(3, senderName)
                it.setObject(4, senderId)
                it.setObject(5, senderPermission)
                it.setObject(6, messageChain.toString().replace(Regex("^\\[mirai:source:\\[\\d+],\\[-?\\d+]]"), ""))
                it.setObject(7, messageChain.serializeToJsonString())
                it.setObject(8, forwarder)
                it.setObject(9, hash)
            }.execute()
            return
        } catch (e: SQLIntegrityConstraintViolationException) {
            return
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }


    fun addString(str: String, time: Int): Boolean {

        var sql = "INSERT INTO qqmessage2.eventsdata " +
                "(time, `event`)" +
                "VALUES (?, ?);"
        try {
            conn!!.prepareStatement(sql).also {
                it.setObject(1, time)
                it.setObject(2, str)
            }.execute()
            return true
        } catch (e: SQLIntegrityConstraintViolationException) {
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    class MessageSearchResult(val time: Int, val msg: String)
}
