package com.celery.rika.commands.callcontrol

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext
import net.mamoe.mirai.console.command.descriptor.EmptyCommandArgumentContext
import net.mamoe.mirai.console.permission.Permission
import org.celery.command.controller.CCommand
import org.celery.command.controller.CommandBasicUsage
import org.celery.command.controller.CommandUsage
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

open class CCompositeCommand(
    owner: CommandOwner,
    primaryName: String,
    vararg secondaryNames: String,
    description: String = "no description available",
    parentPermission: Permission = owner.parentPermission,
    overrideContext: CommandArgumentContext = EmptyCommandArgumentContext,
) : CCommand, CompositeCommand(
    owner, primaryName, secondaryNames = secondaryNames, description, parentPermission, overrideContext
) {
    annotation class Example(val value: String)

    override fun getUsages(): List<CommandBasicUsage> {
        val funcs = this::class.functions.filter { it.annotations.any { it is SubCommand } }.distinctBy {
            it.name
        }
        return funcs.map { subcommand ->
            CommandBasicUsage(
                commandNameDisplay = buildString {
                    if (!prefixOptional) {
                        append(CommandManager.commandPrefix)
                    }
                    append(primaryName)
                    append(" ")
                    if (subcommand.findAnnotation<SubCommand>()!!.value.size <= 1)
                        append(subcommand.findAnnotation<SubCommand>()!!.value.firstOrNull() ?: subcommand.name)
                    else {
                        val subNames = subcommand.findAnnotation<SubCommand>()!!.value.joinToString("|")
                        append("&lt;")
                        append(subNames)
                        append("&gt;")
                    }
                },
                params = subcommand.valueParameters.map {
                    CommandUsage.CommandParam(
                        it.name ?: it.type::class.simpleName ?: "unknow-command-name",
                        it.isOptional
                    )
                },
                description = description,
                superUserUsage = "",
                example = subcommand.findAnnotation<Example>()?.value ?: "",
                commandId = primaryName+"."+subcommand.findAnnotation<SubCommand>()!!.value.first()

            )
        }
    }
}