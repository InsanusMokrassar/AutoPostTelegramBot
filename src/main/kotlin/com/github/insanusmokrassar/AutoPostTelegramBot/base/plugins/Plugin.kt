package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor

import java.util.logging.Level
import java.util.logging.Logger

typealias PluginName = String

val commonLogger = Logger.getLogger("common").also {
    it.level = Level.FINER
}

interface Plugin {
    val name: PluginName
        get() = this::class.java.simpleName

    suspend fun onInit(
        bot: AutoPostTelegramBot
    ) { }
}