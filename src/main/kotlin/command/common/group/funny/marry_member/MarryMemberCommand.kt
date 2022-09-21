package command.common.group.funny.marry_member

import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.funny.marry_member.data.MarryMemberData.getHusband
import command.common.group.funny.marry_member.data.MarryMemberData.getWife
import command.common.group.funny.marry_member.data.MarryMemberData.getXiaoSan
import command.common.group.funny.marry_member.data.MarryMemberData.newMap
import command.common.group.funny.marry_member.model.MarryResult
import command.common.group.funny.marry_member.model.MarryResult.MarryType.Normal
import command.common.group.funny.marry_member.model.MarryResult.MarryType.Single
import events.ExecutionResult
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.cast
import org.celery.command.controller.Call
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.contact.GroupTools
import org.celery.utils.getAvatar
import org.celery.utils.http.HttpUtils.downloader
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts
import java.time.LocalDateTime
import kotlin.random.Random

object MarryMemberCommand : RegexCommand(
    "娶群友", "^娶群友\\s*(.+)?".toRegex(), normalUsage = "娶群友|嫁群友 <TA>", description = "娶群友",
    secondaryRegexs = arrayOf("^嫁群友\\s*(.+)?".toRegex()),
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (newMap[group.id] == null) newMap[group.id] = mutableListOf()

        val (index, match) = eventMatchResult.getIndexedResult()
        val target = GroupTools.getUserOrNull(group, match.groupValues[1].trim())?.cast<Member>()
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

            if (MarryMemberData.isSingle(subject.id, sender.id)) {
                sendMessage("娶勾八，你可是单身贵族\uD83E\uDD23")
                return ExecutionResult.Success
            }
        }
        //目标群友娶了别人或者被娶了，当小三不处理
        if (target != null && MarryMemberData[group.id, target.id] != null) {
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
                    +PlainText("\n[${target.nameCardOrNick}][${target.id}]")
                    +image
                    +PlainText("\n已经娶了[${member1.nameCardOrNick}][${member1.id}]呦~")
                }
                sendMessage(message1)
                return ExecutionResult.Success
            }


            if (MarryMemberData.isSingle(subject.id, target.id)) {
                sendMessage("娶勾八，${target.nameCardOrNick}可是单身贵族🤣")
                return ExecutionResult.Success
            }
        }

        //只在娶成功后设置冷却
        if (target!=null&&setCoolDown(TimeConsts.MIN*20)){
            sendMessage("爬，不许娶了")
            return ExecutionResult.LimitCall
        }
        // 没有指定娶人
        val newMembers = group.members.toMutableList().apply {
            removeAll {
                MarryMemberData.contains(group, it.id)
            }
        }
        val random = Random(LocalDateTime.now().let { it.year + it.dayOfYear + group.id })
        var member1 = newMembers.random(random)
        while (member1.id == sender.id && newMembers.size > 1) {
            member1 = newMembers.random(random)
        }
        if (target?.id==sender.id){
           return marrySelf(sender)

        }
        if (member1.id == sender.id) {
            sendMessage("恭喜你！！你没人要啦哈哈哈哈哈哈哈哈哈哈哈哈！！")
            if (newMap[group.id] == null) newMap[group.id] = mutableListOf()
            newMap[group.id]!!.add(
                MarryResult(
                    sender.id, 0L, Single
                )
            )

            return ExecutionResult.Success
        }
        return if (index == 0)
            marryWife(sender, target ?: member1)
        else
            marryHusband(sender, target ?: member1)
    }

    private suspend fun GroupMessageEvent.marryWife(from: Member, target: Member): ExecutionResult.Success {
        if (newMap[group.id] == null) newMap[group.id] = mutableListOf()
        newMap[group.id]!!.add(
            MarryResult(
                from.id, target.id, Normal
            )
        )

        val message = buildMessageChain {
            +At(from)
            +PlainText("今天你的群老婆是")
            +target.getAvatar(subject)
            +PlainText("[${target.nameCardOrNick}][${target.id}]哒")
        }
        group.sendMessage(message)


        return ExecutionResult.Success
    }

    private suspend fun GroupMessageEvent.marryHusband(from: Member, target: Member): ExecutionResult.Success {
        newMap[group.id]!!.add(MarryResult(from.id, target.id, Normal))
        val image = target.getAvatar(subject)
        val message = buildMessageChain {
            +At(from)
            +PlainText("今天你的群老公是")
            +image
            +PlainText("[${target.nameCardOrNick}][${target.id}]哒")
            // 不可能发生
//            val xiaoSan = target.getXiaoSan()
//            if (xiaoSan != null && xiaoSan.isNotEmpty()) {
//                +(PlainText("\n哦顺便说一下，还有${xiaoSan.size}个人在当他的小三哦~"))
//            }
        }
        group.sendMessage(message)
        return ExecutionResult.Success
    }

    private suspend fun GroupMessageEvent.marrySelf(from: Member): ExecutionResult.Success {
        newMap[group.id]!!.add(MarryResult(from.id, from.id, Single))
        val message = buildMessageChain {
            +At(from)
            +PlainText("我去，自恋狂！！！")
        }
        group.sendMessage(message)
        return ExecutionResult.Success
    }
    override suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {

    }

}




