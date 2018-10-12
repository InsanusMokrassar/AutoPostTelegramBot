package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.pengrad.telegrambot.TelegramBot
import java.util.logging.Level
import java.util.logging.Logger

typealias PluginName = String

val commonLogger = Logger.getLogger("common").also {
    it.level = Level.FINER
}

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