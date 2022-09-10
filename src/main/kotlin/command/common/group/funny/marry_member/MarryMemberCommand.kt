package command.common.group.funny.marry_member

import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.funny.marry_member.data.MarryMemberData.getHusband
import command.common.group.funny.marry_member.data.MarryMemberData.getWife
import command.common.group.funny.marry_member.data.MarryMemberData.getXiaoSan
import command.common.group.funny.marry_member.data.MarryMemberData.newMap
import command.common.group.funny.marry_member.model.MarryResult
import command.common.group.funny.marry_member.model.MarryResult.MarryType.Normal
import command.common.group.funny.marry_member.model.MarryResult.MarryType.XiaoSan
import events.ExecutionResult
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.cast
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.group.GroupTools
import org.celery.utils.http.HttpUtils.downloader
import org.celery.utils.sendMessage
import java.time.LocalDateTime
import kotlin.random.Random

object MarryMemberCommand : RegexCommand(
    "娶群友", "^娶群友\\s*(.+)?".toRegex(), description = "娶群友", secondaryRegexs = arrayOf("^嫁群友\\s*(.+)?".toRegex()),
    normalUsage = "娶群友|嫁群友 <TA>",
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (newMap[group.id] == null) newMap[group.id] = mutableListOf()

        val (index, match) = eventMatchResult.getIndexedResult()
        logger.info("(index, match) = ${eventMatchResult.getIndexedResult().first} ${eventMatchResult.getIndexedResult().second.groupValues}")
        val target = GroupTools.getUserOrNull(group, match.groupValues[1].trim())?.also {
            logger.info("get user $it")
        }?.cast<Member>().also {
            logger.info("get member $it")
        }
        //自己娶了别人或者被娶了，当小三不处理
        if (MarryMemberData[group.id, sender.id] != null) {
            // 被娶了
            val husband = sender.getHusband()
            if (husband != null) {
                val from = group[husband]
                val image = downloader(from!!.avatarUrl).inputStream().toExternalResource().use {
                    group.uploadImage(it)
                }

                buildMessageChain {
                    +At(sender)
                    +PlainText("今天你被娶了,你的群老公是")
                    +image
                    +PlainText("[${from.nameCardOrNick}][${from.id}]哒")
                    val xiaoSan = sender.getXiaoSan()
                    if (xiaoSan != null && xiaoSan.isNotEmpty()) {
                        +(PlainText("\n哦顺便说一下，还有${xiaoSan.size}个人在当他的小三哦，他们是\n${xiaoSan.joinToString("\n") { "[${group[it]?.nameCardOrNick}][${group[it]?.id}]" }}"))
                    }
                }.sendTo(group)
                return ExecutionResult.Success

            }

            // 已经娶过了
            val wife = sender.getWife()
            if (wife != null) {
                val member1 = group[wife]!!
                val image = downloader(member1.avatarUrl).inputStream().toExternalResource().use {
                    group.uploadImage(it)
                }
                val message1 = buildMessageChain {
                    +At(sender)
                    +PlainText("你已经娶了")
                    +image
                    +PlainText("[${member1.nameCardOrNick}][${member1.id}]呦~")
                    val xiaoSan = sender.getXiaoSan()
                    if (xiaoSan != null && xiaoSan.isNotEmpty()) {
                        +(PlainText("\n顺便说一下，还有${xiaoSan.size}个人当你的小三哦，他们是\n${xiaoSan.joinToString("\n") { "[${group[it]?.nameCardOrNick}][${group[it]?.id}]" }}"))
                    }
                }
                sendMessage(message1)
                return ExecutionResult.Success
            }
//            //当小三
//            val qingRen = sender.getQingren()
//            if (qingRen != null) {
//                sendMessage(buildMessageChain {
//                    val member1 = group[qingRen]!!
//                    val image = downloader(member1.avatarUrl).inputStream().toExternalResource().use {
//                        group.uploadImage(it)
//                    }
//                    +At(sender)
//                    +PlainText("今天你已经当了")
//                    +image
//                    +PlainText("[${member1.nameCardOrNick}][${member1.id}]的小三🤗")
//                })
//                return ExecutionResult.Success
//            }
        }
        //目标群友娶了别人或者被娶了，当小三不处理
        if (target!=null&& MarryMemberData[group.id, target.id] != null) {
            // 被娶了
            val husband = target.getHusband()
            if (husband != null) {
                val from = group[husband]
                val image = downloader(from!!.avatarUrl).inputStream().toExternalResource().use {
                    group.uploadImage(it)
                }
                sendMessage(PlainText("今天[${target.nameCardOrNick}][${target.id}]已经被") + image + PlainText("[${from.nameCardOrNick}][${from.id}]娶走啦~"))

                return ExecutionResult.Success
            }

            // 已经娶过了
            val wife = target.getWife()
            if (wife != null) {
                val member1 = group[wife]!!
                val image = downloader(member1.avatarUrl).inputStream().toExternalResource().use {
                    group.uploadImage(it)
                }
                val message1 = buildMessageChain {
                    +At(sender)
                    +PlainText("\n[${member1.nameCardOrNick}][${member1.id}]")
                    +image
                    +PlainText("\n已经被[${target.nameCardOrNick}][${target.id}]娶了呦~")
                }
                sendMessage(message1)
                return ExecutionResult.Success
            }
        }


        // 没有指定娶人
        val newMembers = group.members.toMutableList().apply {
            removeAll {
                MarryMemberData.contains(group, it.id)
            }
        }
        val random = Random(LocalDateTime.now().let { it.year + it.dayOfYear + group.id })
        val member1 = newMembers.random(random)
        return if (index==0)
            marryWife(sender, target?:member1)
        else
            marryHusband(sender,target?:member1)
    }


    private suspend fun GroupMessageEvent.marryWife(from: Member, target: Member): ExecutionResult.Success {
        if (newMap[group.id] == null) newMap[group.id] = mutableListOf()
        newMap[group.id]!!.add(
            MarryResult(
                from.id, target.id, Normal
            )
        )

        val message = buildMessageChain {
            + At(from)
            + PlainText("今天你的群老婆是")
            + getImage(target)
            + PlainText("[${target.nameCardOrNick}][${target.id}]哒")
            //不可能发生
//            val xiaoSan = target.getXiaoSan()
//            if (xiaoSan != null && xiaoSan.isNotEmpty()) {
//                +(PlainText("\n哦顺便说一下，还有${xiaoSan.size}个人在当你的小三哦~"))
//            }
        }
        group.sendMessage(message)


        return ExecutionResult.Success
    }

    private suspend fun GroupMessageEvent.marryHusband(from: Member, target: Member): ExecutionResult.Success {
        newMap[group.id]!!.add(MarryResult(from.id, target.id, Normal))
        val image = getImage(target)
        val message = buildMessageChain {
            + At(from)
            + PlainText("今天你的群老公是")
            + image
            + PlainText("[${target.nameCardOrNick}][${target.id}]哒")
            // 不可能发生
//            val xiaoSan = target.getXiaoSan()
//            if (xiaoSan != null && xiaoSan.isNotEmpty()) {
//                +(PlainText("\n哦顺便说一下，还有${xiaoSan.size}个人在当他的小三哦~"))
//            }
        }
        group.sendMessage(message)
        return ExecutionResult.Success
    }

}

private suspend fun GroupMessageEvent.getImage(target: Member) :Image {
    return downloader(target.avatarUrl).inputStream().toExternalResource().use {
        group.uploadImage(it)
    }
}


object MarryMemberCommandBeXioaSan : RegexCommand(
    "当小三", "^当\\s*(.+)\\s*的小三".toRegex(), description = "当小三",
    normalUsage = "当<TA>的小三",
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val target = GroupTools.getUserOrNull(group, eventMatchResult.getResult().groupValues[1].trim())?.cast<Member>()
        if (target==null){
            sendMessage("没找见这个人欸.")
            return ExecutionResult.LimitCall
        }
        if (newMap[group.id] == null) {
            newMap[group.id] = mutableListOf()
        }
//        if (newMap[group.id]?.any { it.wife == member.id } == true && newMap[group.id]?.any { it.husband == sender.id } == false) {
        if (target.getWife() != null && target.getHusband() == null) {
            val marryResult = MarryResult(
                target.id, sender.id, XiaoSan
            )
            if (newMap[group.id]!!.contains(marryResult)) {
                sendMessage("你已经当了他的小三了😡😡")
                return ExecutionResult.LimitCall
            }
            newMap[group.id]!!.add(
                marryResult
            )
            val message = buildMessageChain {
                + At(sender)
                + PlainText("今天你当了")
                + getImage(target)
                + PlainText("[${target.nameCardOrNick}][${target.id}]的小三~")
                val xiaoSanList = target.getXiaoSan()
                if (xiaoSanList!=null&&xiaoSanList.isNotEmpty()){
                    val xiaoSan = target.getXiaoSan()
                    if (xiaoSan != null && xiaoSan.size>1) {
                        +(PlainText("\n哦顺便说一下，还有${xiaoSan.size-1}个人在和你一起当他的小三哦~"))
                    }
                    if (xiaoSanList.size in 3..4){
                        +PlainText("\n[${target.nameCardOrNick}][${target.id}]这么抢手的吗?")
                    } else if ((xiaoSanList.size>=4)){
                        +PlainText("\n我草你们是疯了吗?😨")
                    }
                }
            }
            group.sendMessage(message)
        } else {
            if (target.getHusband() != null)
                sendMessage("他已经被娶啦")
            else if (target.getWife() == null)
                sendMessage("他还是单身呢")
            else
                sendMessage("unknown stat.")
        }
        return ExecutionResult.Success
    }

}


