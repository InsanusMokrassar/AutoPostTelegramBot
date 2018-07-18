package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.pengrad.telegrambot.TelegramBot

interface PluginManager {
    val plugins: List<Plugin>

    fun byName(name: PluginName): List<Plugin> = plugins.filter {
        it.name == name
    }

    fun byNameAndVersion(name: PluginName, minVersion: PluginVersion): Plugin? = byName(name).filter {
        it.version >= minVersion
    }.maxBy {
        it.version
    }

    fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig
    )
}
