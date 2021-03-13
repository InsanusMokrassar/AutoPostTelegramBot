package dev.inmo.AutoPostTelegramBot.plugins

import dev.inmo.AutoPostTelegramBot.base.models.BotConfig
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.AutoPostTelegramBot.utils.initHandler
import dev.inmo.tgbotapi.bot.RequestsExecutor
import kotlinx.serialization.Serializable

@Serializable
class BotLogger(
    private val config: BotConfig? = null
) : Plugin {
    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val logExecutor = config ?.createBot() ?: executor
        super.onInit(logExecutor, baseConfig, pluginManager)
        initHandler(logExecutor, baseConfig.logsChatId)
    }
}