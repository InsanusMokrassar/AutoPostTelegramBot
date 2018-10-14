package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginConfig
import com.pengrad.telegrambot.TelegramBot
import org.h2.Driver

class Config (
    val targetChatId: Long? = null,
    val sourceChatId: Long? = null,
    val logsChatId: Long? = null,
    val botToken: String? = null,
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    val clientConfig: HttpClientConfig? = null,
    val regen: RequestsRegenConfig? = null,
    val plugins: List<PluginConfig> = emptyList(),
    val commonBot: BotConfig? = null
) {
    private val botConfig: BotConfig by lazy {
        commonBot ?: BotConfig(
            botToken,
            clientConfig,
            regen
        )
    }

    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId ?: throw IllegalArgumentException("Target chat id (field \"targetChatId\") can't be null"),
            sourceChatId ?: throw IllegalArgumentException("Source chat id (field \"sourceChatId\") can't be null"),
            logsChatId ?: sourceChatId,
            botConfig.createBot(),
            databaseConfig,
            plugins
        )
}

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)

class FinalConfig (
    val targetChatId: Long,
    val sourceChatId: Long,
    val logsChatId: Long,
    val bot: TelegramBot,
    val databaseConfig: DatabaseConfig,
    val pluginsConfigs: List<PluginConfig> = emptyList()
)
