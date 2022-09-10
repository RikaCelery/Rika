package org.celery.command.builtin

import command.controller.CSimpleCommand
import net.mamoe.mirai.console.command.BuiltInCommands
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import org.celery.Rika
import org.celery.command.controller.CCommand
import org.celery.utils.permission.isSuperUser

object PanicCommand : CSimpleCommand(
    Rika, "panic"
) {
    private var panic = false
    private var commandList = mutableListOf<net.mamoe.mirai.console.command.Command>()
    private var commandMap = mutableMapOf<CCommand,Boolean>()

    @Handler
    suspend fun CommandSender.on() {
        if (user.isSuperUser()) {
            if (!panic) {
                CommandManager.allRegisteredCommands.forEach {
                    if (it::class.simpleName != "PanicCommand") {
                        println("_ 正在关闭指令${it::class.qualifiedName}")
                        it.unregister()
                        commandList.add(it)
                    }
                }
                Rika.allRegisteredCommand.forEach {
                    if (it::class.simpleName != "PanicCommand") {
                        println("_ 正在关闭指令${it::class.qualifiedName}")
                        commandMap[it] = it.getEnable()
                        it.disable()
                    }
                }
                sendMessage("Zzzz...")
                panic = true
            } else {
                println("正在重新设置rika")
                commandList.forEach {
                    println("_ 正在启用指令${it::class.qualifiedName}")
                    when (it::class.objectInstance) {
                        is BuiltInCommands.HelpCommand -> {}
                        !is PanicCommand -> it.register()
                        is HelpCommand -> it.register(true)
                    }
                }
                commandMap.forEach { (t, u) ->
                    if (u)
                        t.enable()
                }
                sendMessage("（*゜ー゜*）")
                panic = false
                commandList.clear()
            }
        }
    }
}