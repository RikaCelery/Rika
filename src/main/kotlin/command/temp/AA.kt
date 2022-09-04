package org.celery.command.temp

import com.celery.rika.commands.callcontrol.CCompositeCommand
import net.mamoe.mirai.console.command.CommandSender
import org.celery.Rika

object AA:CCompositeCommand(
    Rika,"aa"
) {
    @SubCommand("bb","cc")
    suspend fun CommandSender.handle(){
        println("aa bb/cc")
        AA.getUsages().map { it.commandId }
    }
}