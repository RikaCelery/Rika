package org.celery.utils.contact

import net.mamoe.mirai.contact.*
import org.celery.Rika

/**
 * @author laolittle
 */
// https://github.com/LaoLittle
object GroupTools {
    /**
     * 通过消息获取联系人
     * 若[Contact]为[Group]，则可通过群员昵称获取联系人
     * 否则通过QQ号查找，查找失败返回``null``
     * @param msg 传入的消息[String]
     * @return User if only one is found null otherwise
     * */
    fun getUserOrNull(group: Contact,msg: String): User? {
        Rika.logger.debug(msg)
        val noneAt = msg.replace("@", "")
        if (noneAt.isBlank()) {
            return null
        }
        return if (noneAt.contains(Regex("""\D"""))) {
            when (group) {
                is Group -> group.findMemberOrNull(noneAt)
                else -> null
            }
        } else {
            val number = noneAt.toLong()
            when (group) {
                is Group -> group[number]
                else -> group.bot.getFriend(number) ?: group.bot.getStranger(number)
            }
        }
    }
    fun getMemberOrNull(group: Group,msg: String): Member? {
        Rika.logger.debug(msg)
        val noneAt = msg.replace("@", "")
        if (noneAt.isBlank()) {
            return null
        }
        return if (noneAt.contains(Regex("""\D"""))) {
            group.findMemberOrNull(noneAt)
        } else {
            val number = noneAt.toLong()
            group[number]
        }
    }
    /**
     * 从一个群中模糊搜索昵称是[nameCard]的群员
     * @param nameCard 群员昵称
     * @return Member if only one exist or null otherwise
     * @author mamoe
     * */
    fun Group.findMemberOrNull(nameCard: String): Member? {
        this.members.singleOrNull { it.nameCardOrNick.contains(nameCard) }?.let { return it }
        this.members.singleOrNull { it.nameCardOrNick.contains(nameCard, ignoreCase = true) }?.let { return it }

        val candidates = this.fuzzySearchMember(nameCard)
        candidates.singleOrNull()?.let {
            if (it.second == 1.0) return it.first // single match
        }
        if (candidates.isNotEmpty()) {
            var index = 1
            println(
                "无法找到成员 $nameCard。 多个成员满足搜索结果或匹配度不足: \n\n" +
                        candidates.joinToString("\n", limit = 6) {
                            val percentage = (it.second * 100).toDecimalPlace(0)
                            "#${index++}(${percentage}%)${it.first.nameCardOrNick.truncate(10)}(${it.first.id})" // #1 15.4%
                        }
            )
        }
        return null
    }
}