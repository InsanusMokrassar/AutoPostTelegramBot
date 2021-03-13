package dev.inmo.AutoPostTelegramBot.base.plugins

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.tgbotapi.bot.RequestsExecutor

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

inline fun <reified T: Plugin> PluginManager.findFirstPlugin(): T? = plugins.firstOrNull {
    it is T
} as? T
