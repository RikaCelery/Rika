package org.celery.command.builtin

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import org.celery.Rika

object FunctionCallControl:SimpleCommand(
    Rika,"test"
) {
    @Handler
    suspend fun CommandSender.handle(){

    }
}