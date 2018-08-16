package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.pengrad.telegrambot.TelegramBot
import java.util.logging.Logger

typealias PluginVersion = Long
typealias PluginName = String

val commonLogger = Logger.getAnonymousLogger()

@Deprecated(
    "This variable was deprecated for the reason that it is useless in context of commonarchitecture",
    ReplaceWith("commonLogger")
)
val pluginLogger = commonLogger

interface Plugin {
    val name: PluginName
        get() = this::class.java.simpleName

    fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) { }
}