package org.celery

object PluginManager {

    fun add(plugin: Plugin) {
        plugins.add(plugin)
    }

    val plugins = mutableListOf<Plugin>()
}
