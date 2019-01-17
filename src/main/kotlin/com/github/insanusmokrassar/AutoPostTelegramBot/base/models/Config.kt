package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.ListSerializer
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import org.h2.Driver

@Serializable
class Config (
    val botToken: String,
    val targetChatId: Long,
    val sourceChatId: Long,
    @Optional
    val logsChatId: Long = sourceChatId,
    @Optional
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    @Optional
    val clientConfig: HttpClientConfig? = null,
    @Serializable(ListSerializer::class)
    @Optional
    val plugins: List<Plugin> = emptyList(),
    @Optional
    val commonBot: BotConfig? = null
) {
    private val botConfig: BotConfig by lazy {
        commonBot ?: BotConfig(
            botToken,
            clientConfig
        )
    }

    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId.toChatId(),
            sourceChatId.toChatId(),
            logsChatId.toChatId(),
            botConfig.createBot(),
            databaseConfig,
            plugins
        )
}

class FinalConfig (
    val targetChatId: ChatId,
    val sourceChatId: ChatId,
    val logsChatId: ChatId,
    val bot: RequestsExecutor,
    val databaseConfig: DatabaseConfig,
    val pluginsConfigs: List<Plugin> = emptyList()
)
