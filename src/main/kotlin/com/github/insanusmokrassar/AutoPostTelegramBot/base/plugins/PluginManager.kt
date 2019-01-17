package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor

interface PluginManager {
    val plugins: List<Plugin>

    fun byName(name: PluginName): List<Plugin> = plugins.filter {
        it.name == name
    }

    suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig
    )
}
