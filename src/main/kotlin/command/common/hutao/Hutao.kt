package org.celery.command.common.hutao

import org.celery.command.controller.abs.Command


/**
 * 胡桃的kt实现
 * 仅可用于群聊
 */
object Hutao:Command(
    "胡桃"
) {

    /**
     * 一个子功能
     *
     * 功能：对于匹配某一个正则的消息
     *
     * 回复某些消息
     *
     * 对变量进行某些操作
     */
    class Function(
        val regex: String,
        val reply: MutableList<Reply> = mutableListOf()
    )

    interface Action {

    }

    class Reply(
        val regex: String,
        val actionStr:String,
        val weight:Int=1,
        val percentage:Double = 1.0
    ){
       fun toActions():List<Action>{
           return listOf()//TODO
       }
    }

    fun test(){
    }
}
