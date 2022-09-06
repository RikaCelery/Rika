package org.celery.command.builtin

import com.celery.rika.commands.callcontrol.CSimpleCommand
import net.mamoe.mirai.console.command.BuiltInCommands
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.apache.commons.lang3.StringEscapeUtils
import org.celery.Rika
import org.celery.command.controller.Call
import org.celery.command.controller.CommandBasicUsage
import org.celery.command.controller.CommandUsage
import org.celery.command.controller.Limitable
import org.celery.utils.file.FileTools
import org.celery.utils.permission.isSuperUser
import org.celery.utils.selenium.Selenium

@OptIn(ExperimentalCommandDescriptors::class, ConsoleExperimentalApi::class)
object HelpCommand : CSimpleCommand(
    Rika, "help", secondaryNames = arrayOf("帮助", "菜单")
) {
    val selenium by lazy { Selenium(false) }

    @Handler
    suspend fun CommandSender.on(name: String? = null) {
        if (subject == null) println(BuiltInCommands.HelpCommand.generateDefaultHelp(this.permitteeId))
        if (name == null) sendMessage(renderAllToHtml(this.permitteeId).toImageOrPlainText(this))
    }

    private suspend fun List<CommandUsage>.toImageOrPlainText(commandSender: CommandSender): Message {
        if (commandSender.subject == null) return PlainText(this.toString())
        val tempFile = FileTools.creatTempFile("html")

        val html = """
<body>
    <ul>
        ${this.joinToString("") { it.renderToLi() }}
    </ul>
</body>
<style>
    head1 {
        font-size: 40pt;
    }

    body {
        background-color: rgb(6, 72, 112);
        color: aliceblue;
        font-family: Arial, Helvetica, sans-serif;
    }

    div.command-name {
        color: rgb(255, 255, 255);
        font-weight: bold;

    }
    li.no-permission{
        display: none;
    }
    li.disable  > * {
        text-decoration-line: line-through;
        text-decoration-color: rgb(246, 0, 0);
        font-weight: bold;
    }
    div.command-name > must {
        margin-left: 3px;
        color: #33d3ff;
        font-weight: lighter;
        background-color: #33547e62;
        border-radius: 9px;
        padding-top: 1px;
        padding-bottom: 2px;
        padding-left: 4px;
        padding-right: 5px;
        background-blend-mode: darken;

    }


    div.command-name > optional {
        margin-left: 3px;
        color: #b1b1b1;
        font-weight: lighter;
        background-color: #396a3d98;
        border-radius: 9px;
        padding-top: 1px;
        padding-bottom: 2px;
        padding-left: 4px;
        padding-right: 5px;
        background-blend-mode: darken;

    }
    div.command-description {
        color: rgb(93, 201, 255);
        margin-left: 20px;
        font-weight: lighter;
        margin-top: 5px;
        margin-bottom: 5px;
        border-radius: 13px;
        padding-left: 10px;
        padding-bottom: 4px;
        background-color: rgb(54, 97, 123);
        box-shadow: 2px 2px 6px #5a5a5a81;
    }

    div.command-usage {
        font-weight: lighter;
        margin-left: 40px;
        padding: 8px;
        color: rgb(255, 255, 255);
        background-color: rgb(54, 97, 123);
        box-shadow: 2px 2px 6px #5a5a5a81;
        border-color: initial;
        /* border-style: inset; */
        border-width: 4px;
        border-radius: 13px;
    }

    ul {
        margin-right: 5px;
        margin-top: 15px;
        font-size: 25px;
    }

    ul>li {
        /* border: groove; */
        border-radius: 15px;
        margin-bottom: 10px;
        margin-top: 20px;
        padding: 10px;
        /* border-width: 1px; */
        background-color: rgb(70, 125, 159);

        text-shadow: 2px 2px 4px #0c0c0cb7;
        box-shadow: 6px 6px 20px #ffffff3a;
    }
</style>
    """
        tempFile.writeText(html)
        selenium.get("file:///${tempFile.absolutePath}")
        val file = selenium.screenShotElementOrAll()
        //dimension = Dimension(600, 80 + 60 * lines.size)
        return file.readBytes().toExternalResource().use {
            commandSender.subject!!.uploadImage(it)
        }
    }

    private fun CommandSender.renderAllToHtml(permitteeId: PermitteeId?): List<CommandUsage> {
        return CommandManager.allRegisteredCommands.mapNotNull {
//            it.usage.lines().map { line ->
//                CommandUsage(
//                    CommandBasicUsage(
//                        StringEscapeUtils.escapeHtml4(line.split("#").first()),
//                        listOf(),
//                        StringEscapeUtils.escapeHtml4(line.split("#").last()),
//                        "",
//                        "",
//                        ""
//                    ),
//                    permitteeId?.hasPermission(it.permission) == true,
//                    permitteeId?.hasPermission(it.permission) == true,
//                )
//            }
            mutableListOf<CommandUsage>()
        }.flatten().toMutableList().apply {
            addAll(Rika.allRegisterdCommand.map {
                it.getUsages().map { basicUsage ->

                    val call = Call(
                        basicUsage.commandId, user?.id, subject?.id
                    )
                    val cast = it.safeCast<Limitable>()
                    val hasPermission = cast?.canCall(call) ?: true
                    CommandUsage(
                        basicUsage,
                        hasPermission,
                        user.isSuperUser() || user.safeCast<NormalMember>()?.isOperator() == true
                    )
                }
            }.flatten())
        }
    }


}
