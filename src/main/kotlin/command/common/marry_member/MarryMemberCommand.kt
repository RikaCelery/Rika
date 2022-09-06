package command.common.marry_member

import events.ExecutionResult
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.command.common.marry_member.data.MarryMemberData
import org.celery.command.common.marry_member.data.MarryMemberData.getHusband
import org.celery.command.common.marry_member.data.MarryMemberData.getWife
import org.celery.command.common.marry_member.data.MarryMemberData.newMap
import org.celery.command.common.marry_member.model.MarryResult
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.group.GroupTools
import org.celery.utils.http.HttpUtils.downloader
import org.celery.utils.sendMessage
import java.time.LocalDateTime
import java.util.*

object MarryMemberCommand : RegexCommand(
    "娶群友", "^娶群友\\s*(.+)?".toRegex(), description = "娶群友"
) {
    /*
     * groupId :
     *   B : A ,B is married by A
     */


    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult) {
        val member = GroupTools.getUserOrNull(group, eventMatchResult.getResult().groupValues[1])
        // 被娶了
        val husband = sender.getHusband()
        if (husband != null) {
            val from = group[husband]
            val image = downloader(from!!.avatarUrl).inputStream().toExternalResource().use {
                group.uploadImage(it)
            }
            sendMessage(PlainText("今天你被娶了,你的群老公是") + image + PlainText("[${from.nameCardOrNick}][${from.id}]哒"))
            return
        }
        // 已经娶过了
        val wife = sender.getWife()
        if (wife != null) {
            val member1 = group[wife]!!
            val image = downloader(member1.avatarUrl).inputStream().toExternalResource().use {
                group.uploadImage(it)
            }
            sendMessage(At(sender) + PlainText("你已经娶了") + image + PlainText("[${member1.nameCardOrNick}][${member1.id}]呦~"))
            return
        }
        // 指定娶某人
        if (member != null) {
            // 娶的人已经被娶了
            val wife1 = sender.getHusband()
            if (wife1 != null) {
                val member1 = group[wife1]
                if (member1 == null)
                    sendMessage(PlainText("[${member.nameCardOrNick}][${member.id}]已经被[${wife1}]娶了,可惜他已经离开了这里"))
                else {
                    val image = downloader(member1.avatarUrl).inputStream().toExternalResource().use {
                        group.uploadImage(it)
                    }
                    sendMessage(PlainText("[${member.nameCardOrNick}][${member.id}]已经被") + image + PlainText("[${member1.nameCardOrNick}][${member1.id}]娶了呦~"))
                }
                return
            }
            // 娶的人已经娶了别人
            val wife2 = sender.getWife()
            if (wife2 != null) {
//                val targetIEntry = marriedMap[group.id]?.entries?.find { it.value == member.id }
//                checkNotNull(targetIEntry) { "can not find entry where value=" + member.id + " in marry map." }
                val target =
                    //娶了别人,key一定存在,但是如果退群,members就找不见
                    group[wife2]
                checkNotNull(target) { "can not find wife: $wife2 in group members." }
                val image = downloader(target.avatarUrl).inputStream().toExternalResource().use {
                    group.uploadImage(it)
                }
                sendMessage(
                    At(sender) + PlainText("[${member.nameCardOrNick}][${member.id}]已经娶了") + image + PlainText(
                        "[${target.nameCardOrNick}][${target.id}]呦~"
                    )
                )
                return
            }
            // 娶自己
            if (member.id == sender.id) {
                sendMessage(PlainText("这样好吗？"))
                delay(1000)
            }

            newMarry(from = sender, target = member as Member)
            return
        }
        // 没有指定娶人
        val newMembers = group.members.toMutableList().apply {
            removeAll {
                MarryMemberData.contains(group, it.id)
            }
        }
        val random = Random(
            LocalDateTime.now()
                .let { it.year + it.dayOfYear + group.id }
        )
        val index = random.nextInt(newMembers.size)
        val member1 = newMembers.shuffled(random)[index]
        newMarry(sender, member1)

    }

    private suspend fun GroupMessageEvent.newMarry(from: Member, target: Member) {
        if (newMap[group.id] == null)
            newMap[group.id] = mutableListOf()
        newMap[group.id]!!.add(
            MarryResult(
                from.id,
                target.id,
                MarryResult.MarryType.Normal
            )
        )

        val image = downloader(target.avatarUrl).inputStream().toExternalResource().use {
            group.uploadImage(it)
        }
        group.sendMessage(At(from) + PlainText("今天你的群老婆是") + image + PlainText("[${target.nameCardOrNick}][${target.id}]哒"))

    }
}

object MarryMemberCommandBeXioaSan : RegexCommand(
    "当小三", "^当\\s*(.+)\\s*的小三".toRegex(), description = "当小三"
) {
    /*
     * groupId :
     *   B : A ,B is married by A
     */


    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val member = GroupTools.getUserOrNull(group, eventMatchResult.getResult().groupValues[1])
        if (member == null) {
            return ExecutionResult.Ignored("target member not found.")
        }
        if (newMap[group.id] == null) {
            newMap[group.id] = mutableListOf()
        }
        if (newMap[group.id]?.any { it.wife == member.id } == true && newMap[group.id]?.any { it.husband == sender.id } == false) {
            newMap[group.id]!!.add(
                MarryResult(
                    sender.id,
                    member.id,
                    MarryResult.MarryType.XiaoSan
                )
            )
            val image = downloader(member.avatarUrl).inputStream().toExternalResource().use {
                group.uploadImage(it)
            }
            group.sendMessage(At(sender) + PlainText("今天你当了") + image + PlainText("[${member.nameCardOrNick}][${member.id}]的小三~"))

        } else {
            return ExecutionResult.Ignored("target member not marraied.")
        }
        return ExecutionResult.Success()
    }

//    private suspend fun GroupMessageEvent.newMarry(from: Member, to: Member, type: MarryResult.MarryType) {
//        marriedMap[group.id].apply {
//            if (this == null)
//                marriedMap[group.id] = mutableMapOf(to.id to from.id)
//            else
//                this[to.id] = from.id
//        }
//
//        val image = downloader(to.avatarUrl).inputStream().toExternalResource().use {
//            group.uploadImage(it)
//        }
//        group.sendMessage(At(from) + PlainText("今天你的群老婆是") + image + PlainText("[${to.nameCardOrNick}][${to.id}]哒"))
//
//    }
}