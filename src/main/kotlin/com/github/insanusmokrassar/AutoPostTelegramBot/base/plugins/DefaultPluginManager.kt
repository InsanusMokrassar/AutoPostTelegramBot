package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*

class DefaultPluginManager(
    pluginsCollection: Collection<Plugin>
) : PluginManager {
    override val plugins: List<Plugin> = pluginsCollection.toList()

    override suspend fun onInit(bot: AutoPostTelegramBot) {
        coroutineScope {
            plugins.map {
                launch {
                    it.onInit(bot)
                    commonLogger.info("Plugin ${it.name} was init")
                }
            }
        }.joinAll()
        commonLogger.info("Plugins was initiated")
    }
}