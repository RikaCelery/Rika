package command.controller

import net.mamoe.mirai.console.command.Command.Companion.allNames
import net.mamoe.mirai.console.command.CommandOwner
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.descriptor.CommandArgumentContext
import net.mamoe.mirai.console.command.descriptor.EmptyCommandArgumentContext
import net.mamoe.mirai.console.permission.Permission
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.BlockRunMode.Subject
import org.celery.command.controller.BlockRunMode.User
import org.celery.command.controller.CCommand
import org.celery.command.controller.CommandBasicUsage
import org.celery.command.controller.CommandUsage
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

@Suppress("unused")
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
    override var showTip: Boolean = true

    annotation class Example(val value: String)

    override fun getUsages(): List<CommandBasicUsage> {
        val func = this::class.functions.single { it -> it.annotations.any { it is Handler } }
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
                        it.name ?: it.type::class.simpleName ?: "unknown-command-name",
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

    override var defaultCountLimit: Int = 10
    override val commandId: String = primaryName
    override var defaultCallCountLimitMode: BlockRunMode = User
    override var defaultCoolDown: Long = 3
    override var defaultBlockRunModeMode: BlockRunMode = Subject
    override var defaultEnable: Boolean = true

}