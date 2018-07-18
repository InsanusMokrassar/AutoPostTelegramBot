package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.pengrad.telegrambot.TelegramBot
import java.util.logging.Logger

typealias PluginVersion = Long
typealias PluginName = String

val pluginLogger = Logger.getLogger(Plugin::class.java.simpleName)

interface Plugin {
    val version: PluginVersion
    val name: PluginName
        get() = this::class.java.simpleName

    fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    )
}