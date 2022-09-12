package command.common.group.funny.marry_member.data

import command.common.group.funny.marry_member.model.MarryResult
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member

object MarryMemberData:AutoSavePluginData("MarryMemberData"){
    val newMap:MutableMap<Long,MutableList<MarryResult>> by value()

    operator fun get(groupId:Long,userId:Long): MarryResult? {
        return newMap[groupId]?.find { it.contains(userId) }
    }



    fun Member.getWife(): Long? {
        return newMap[this.group.id]?.singleOrNull { it.husband==id&&it.type== MarryResult.MarryType.Normal }?.wife
    }
    fun Member.getHusband() = newMap[this.group.id]?.singleOrNull { it.wife==id&&it.type== MarryResult.MarryType.Normal }?.husband
    fun Member.getXiaoSan() = newMap[this.group.id]?.filter { it.husband==id&&it.type== MarryResult.MarryType.XiaoSan }?.map{ it.wife }?.ifEmpty { null }
    fun Member.getQingren() = newMap[this.group.id]?.singleOrNull { it.wife==id&&it.type== MarryResult.MarryType.XiaoSan }?.husband
    fun contains(group: Group,id: Long) = newMap[group.id]?.any { it.contains(id) }==true
    fun isSingle(groupId: Long, id: Long): Boolean {
        return newMap[groupId]?.any { it.husband==id&&it.type==MarryResult.MarryType.Single }?:false
    }
}