package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.BotConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.initHandler
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
class BotLogger(
    @Optional
    private val config: BotConfig? = null
) : Plugin {
    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val logExecutor = config ?.createBot() ?: executor
        super.onInit(logExecutor, baseConfig, pluginManager)
        initHandler(logExecutor, baseConfig.logsChatId)
    }
}