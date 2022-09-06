package org.celery.command.common.marry_member.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import org.celery.command.common.marry_member.model.MarryResult

object MarryMemberData:AutoSavePluginData("MarryMemberData"){
    val marriedMap:MutableMap<Long,MutableMap<Long,Long>> by value()
    val newMap:MutableMap<Long,MutableList<MarryResult>> by value()
    fun Member.getWife() = newMap[this.group.id]?.find { it.husband==id&&it.type==MarryResult.MarryType.Normal }?.wife
    fun Member.getHusband() = newMap[this.group.id]?.find { it.wife==id&&it.type==MarryResult.MarryType.Normal }?.husband
    fun Member.getXiaoSan() = newMap[this.group.id]?.filter { it.wife==id&&it.type==MarryResult.MarryType.XiaoSan }?.map{ it.husband }?.ifEmpty { null }
    fun contains(group: Group,id: Long) = newMap[group.id]?.any { it.contains(id) }==true
}