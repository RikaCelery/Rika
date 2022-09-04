package org.celery.command.controller

/**
 * 可生成用法
 */
interface CommandInfo {
    fun getUsages(): List<CommandBasicUsage>
}