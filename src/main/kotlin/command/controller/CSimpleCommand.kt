package com.celery.rika.commands.callcontrol

import net.mamoe.mirai.console.command.Command.Companion.allNames
import net.mamoe.mirai.console.command.CommandOwner
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext
import net.mamoe.mirai.console.command.descriptor.EmptyCommandArgumentContext
import net.mamoe.mirai.console.permission.Permission
import org.celery.command.controller.*
import org.celery.command.controller.BlockRunMode.User
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

/**
 * 一个简单指令，不能进行调用控制
 */
open class CSimpleCommand(
    owner: CommandOwner,
    primaryName: String,
    vararg secondaryNames: String,
    description: String = "no description available",
    parentPermission: Permission = owner.parentPermission,
    overrideContext: CommandArgumentContext = EmptyCommandArgumentContext,
) : CCommand, SimpleCommand(
    owner, primaryName, secondaryNames = secondaryNames, description, parentPermission, overrideContext
) {

    annotation class Example(val value: String)

    override fun getUsages(): List<CommandBasicUsage> {
        val func = this::class.functions.single { it.annotations.any { it is Handler } }
        val example = func.annotations.filterIsInstance<Example>().singleOrNull()
        val prefix = if (prefixOptional) "" else "/"
        return listOf(
            CommandBasicUsage(
                commandNameDisplay = buildString {
                    append(prefix)
                    if (allNames.size == 1)
                        append(primaryName)
                    else {
                        append(allNames.joinToString("|"))
                    }
                },
                params = func.valueParameters.map {
                    CommandUsage.CommandParam(
                        it.name ?: it.type::class.simpleName ?: "unknow-command-name",
                        it.isOptional
                    )
                },
                description = description,
                superUserUsage = "",
                example = example?.value ?: "",
                commandId = primaryName
            )
        )
    }

    override var defultCountLimit: Int = 10
    override val commandId: String = primaryName
    override var defultCallCountLimitMode: BlockRunMode = User
    override var defultMinCooldown: Int = 3
    override var defultEnable: Boolean = true

}