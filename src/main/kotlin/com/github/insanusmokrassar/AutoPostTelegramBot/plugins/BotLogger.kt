package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
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
    override suspend fun onInit(bot: AutoPostTelegramBot) {
        val logExecutor = config ?.createBot() ?: bot.executor
        super.onInit(bot)
        initHandler(logExecutor, bot.config.logsChatId)
    }
}