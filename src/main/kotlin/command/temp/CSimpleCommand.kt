package org.celery.command.temp//package com.example.command
//
//import com.celery.rika.commands.builtin.CommandBasicUsage
//import com.celery.rika.commands.builtin.CommandUsage
//import com.celery.rika.events.ExecutionResult
//import net.mamoe.mirai.console.command.Command.Companion.allNames
//import net.mamoe.mirai.console.command.CommandOwner
//import net.mamoe.mirai.console.command.CommandSender
//import net.mamoe.mirai.console.command.SimpleCommand
//import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext
//import net.mamoe.mirai.console.command.descriptor.EmptyCommandArgumentContext
//import net.mamoe.mirai.console.permission.Permission
//import net.mamoe.mirai.console.util.safeCast
//import java.util.*
//import kotlin.reflect.full.functions
//import kotlin.reflect.full.valueParameters
//
//open class CSimpleCommand(
//    owner: CommandOwner,
//    primaryName: String,
//    vararg secondaryNames: String,
//    description: String = "no description available",
//    parentPermission: Permission = owner.parentPermission,
//    overrideContext: CommandArgumentContext = EmptyCommandArgumentContext,
//) : Command, Limitable, SimpleCommand(
//    owner, primaryName, secondaryNames = secondaryNames, description, parentPermission, overrideContext
//) {
//
//    annotation class Example(val value: String)
//
//    override fun getUsages(): List<CommandBasicUsage> {
//        val func = this::class.functions.single { it.annotations.any { it is Handler } }
//        val example = func.annotations.filterIsInstance<Example>().singleOrNull()
//        val prefix = if (prefixOptional) "" else "/"
//        return listOf(
//            CommandBasicUsage(
//                commandNameDisplay = buildString {
//                    append(prefix)
//                    if (allNames.size == 1)
//                        append(primaryName)
//                    else {
//                        append(allNames.joinToString("|"))
//                    }
//                },
//                params = func.valueParameters.map {
//                    CommandUsage.CommandParam(
//                        it.name ?: it.type::class.simpleName ?: "unknow-command-name",
//                        it.isOptional
//                    )
//                },
//                description = description,
//                example = example?.value ?: "",
//                commandId = primaryName
//            )
//        )
//    }
//
//    override fun canCall(call: Call): Boolean {
//        return super.canCall(call)
//    }
//
//    override fun hasPermission(call: Call): Boolean {
//        return super.hasPermission(call)
//    }
//
//    override lateinit var defultCountC: Hashtable<String, Int>
//    override var defultCountLimit: Int = 10
//    override val commandId: String = primaryName
//    override var defultCallCountLimitMode: Command.BlockRunMode = Command.BlockRunMode.User
//    override var defultMinCooldown: Int = 3
//    override lateinit var callCd: Hashtable<String, Long>
//    override lateinit var callCountLimit: Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>
//    override lateinit var blackListUser: Hashtable<Long, MutableList<Long>>
//    override lateinit var blackListGroup: MutableList<Long>
//    override lateinit var whiteListUser: Hashtable<Long, MutableList<Long>>
//    override lateinit var whiteListGroup: MutableList<Long>
//    override var enable: Boolean = true
//    override var defultEnable: Boolean = true
//    override lateinit var blockMode: Hashtable<Long, Command.CommandBlockMode>
//    override suspend fun CommandSender.execute(block: suspend CommandSender.() -> Any) {
//        val call = Call(commandId, user?.id, subject?.id)
//        if (canCall(call)) {
//            val result = block()
//            val safeCast = result.safeCast<ExecutionResult>()
//            if (safeCast is ExecutionResult.Success) {
//                addCall(call)
//            }
//        }
//    }
//
//}