package org.celery.command.temp//package com.example.command
//
//import com.celery.rika.commands.builtin.CommandBasicUsage
//import com.celery.rika.commands.builtin.CommandUsage
//import net.mamoe.mirai.console.command.CommandManager
//import net.mamoe.mirai.console.command.CommandOwner
//import net.mamoe.mirai.console.command.CommandSender
//import net.mamoe.mirai.console.command.CompositeCommand
//import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext
//import net.mamoe.mirai.console.command.descriptor.EmptyCommandArgumentContext
//import net.mamoe.mirai.console.permission.Permission
//import java.util.*
//import kotlin.reflect.KParameter
//import kotlin.reflect.full.findAnnotation
//import kotlin.reflect.full.functions
//import kotlin.reflect.full.valueParameters
//
//open class CCompositeCommand(
//    owner: CommandOwner,
//    primaryName: String,
//    vararg secondaryNames: String,
//    description: String = "no description available",
//    parentPermission: Permission = owner.parentPermission,
//    overrideContext: CommandArgumentContext = EmptyCommandArgumentContext,
//) : Command, CompositeCommand(
//    owner, primaryName, secondaryNames = secondaryNames, description, parentPermission, overrideContext
//) {
//    annotation class Example(val value: String)
//    val subCommandNames = this::class.functions.filter { it.annotations.any { it is SubCommand } }
//        .map { it.annotations.filterIsInstance<SubCommand>().singleOrNull()?.value?: arrayOf(it.name) }.flatMap { it.toList() }
//
//    val simpeUsage: String by lazy {
//        val funcs = this::class.functions.filter { it.annotations.any { it is SubCommand } }.distinctBy {
//            it.name
//        }
//        val maped = funcs.map { subcommand ->
//            buildString {
//                if (!prefixOptional) {
//                    append(CommandManager.commandPrefix)
//                }
//                println("prime name is $primaryName")
//                append(primaryName)
//                append(" ")
//                if (subcommand.findAnnotation<SubCommand>()!!.value.size <= 1)
//                    append(subcommand.name)
//                else {
//                    val subNames = subcommand.findAnnotation<SubCommand>()!!.value.joinToString("|")
//                    append('<')
//                    append(subNames)
//                    append('>')
//                }
//                append(" ")
//                append(subcommand.valueParameters.joinToString(" ") { it.render() })
//                val annotion = subcommand.findAnnotation<Description>()?.value ?: "no description available"
//                append("    # ")
//                append(annotion)
//            }
//        }
//        maped.joinToString("\n")
//    }
//    val detailedUsageHtml: String by lazy {
//        val funcs = this::class.functions.filter { it.annotations.any { it is SubCommand } }.distinctBy {
//            it.name
//        }
//        val maped = funcs.map { subcommand ->
//            buildString {
//                if (!prefixOptional) {
//                    append(CommandManager.commandPrefix)
//                }
//                println("prime name is $primaryName")
//                append(primaryName)
//                append(" ")
//                if (subcommand.findAnnotation<SubCommand>()!!.value.size <= 1)
//                    append(subcommand.name)
//                else {
//                    val subNames = subcommand.findAnnotation<SubCommand>()!!.value.joinToString("|")
//                    append("&lt;")
//                    append(subNames)
//                    append("&gt;")
//                }
//                append(" ")
//                append(subcommand.valueParameters.joinToString(" ") { it.renderHtml() })
//                val annotion = subcommand.findAnnotation<Description>()?.value ?: "no description available"
//                append("<br>   ")
//                append(annotion)
//                val example = subcommand.findAnnotation<Example>()?.value
//                if (example != null) {
//                    append("<br>   例子:<br>")
//                    append(example)
//                }
//            }
//        }
//        maped.joinToString("\n").replace("\n", "<br>").replace(" ", "&nbsp;")
//    }
//
//    private fun KParameter.render(): String {
//        return if (isOptional) "[${name}]" else "<${name ?: type::class.simpleName}>"
//    }
//
//    private fun KParameter.renderHtml(): String {
//        return if (isOptional) "[${name}]" else "&lt;${name ?: type::class.simpleName}&gt;"
//    }
//
//    override fun getUsages(): List<CommandBasicUsage> {
//        val funcs = this::class.functions.filter { it.annotations.any { it is SubCommand } }.distinctBy {
//            it.name
//        }
//        return funcs.map { subcommand ->
//            CommandBasicUsage(
//                commandNameDisplay = buildString {
//                    if (!prefixOptional) {
//                        append(CommandManager.commandPrefix)
//                    }
//                    append(primaryName)
//                    append(" ")
//                    if (subcommand.findAnnotation<SubCommand>()!!.value.size <= 1)
//                        append(subcommand.findAnnotation<SubCommand>()!!.value.firstOrNull() ?: subcommand.name)
//                    else {
//                        val subNames = subcommand.findAnnotation<SubCommand>()!!.value.joinToString("|")
//                        append("&lt;")
//                        append(subNames)
//                        append("&gt;")
//                    }
//                },
//                params = subcommand.valueParameters.map {
//                    CommandUsage.CommandParam(
//                        it.name ?: it.type::class.simpleName ?: "unknow-command-name",
//                        it.isOptional
//                    )
//                },
//                description = description,
//                example = subcommand.findAnnotation<Example>()?.value ?: "",
//                commandId = ""
//
//            )
//        }
//
//    }
//
//    override lateinit var defultCountC: Hashtable<String, Int>
//    override lateinit var callCd: Hashtable<String, Long>
//    override lateinit var callCountLimit: Hashtable<String, MutableMap<Long, MutableMap<Long, Int>>>
//    override lateinit var blackListUser: Hashtable<Long, MutableList<Long>>
//    override lateinit var blackListGroup: MutableList<Long>
//    override lateinit var whiteListUser: Hashtable<Long, MutableList<Long>>
//    override lateinit var whiteListGroup: MutableList<Long>
//    override var enable: Boolean = true
//    override lateinit var blockMode: Hashtable<Long, Command.CommandBlockMode>
//    override suspend fun CommandSender.execute(block: suspend CommandSender.() -> Any) {
//    }
//}