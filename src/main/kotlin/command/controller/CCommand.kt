package org.celery.command.controller

import net.mamoe.mirai.console.command.Command

interface CCommand: Command,Limitable,CommandInfo {
    override val commandId: String
        get() = TODO("Not yet implemented")
    override var defultEnable: Boolean
        get() = true
        set(value) {}
    override var defultCountLimit: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var defultCallCountLimitMode: BlockRunMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var defultMinCooldown: Int
        get() = TODO("Not yet implemented")
        set(value) {}
}