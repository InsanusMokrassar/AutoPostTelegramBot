package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.ListSerializer
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.toChatId
import kotlinx.serialization.*
import org.h2.Driver

@Serializable
class Config (
    val targetChatId: Long,
    val sourceChatId: Long,
    @Optional
    val logsChatId: Long? = null,
    @Optional
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    @Optional
    val clientConfig: HttpClientConfig? = null,
    @Optional
    val botToken: String? = null,
    @Serializable(ListSerializer::class)
    @Optional
    val plugins: List<Plugin> = emptyList(),
    @Optional
    val commonBot: BotConfig? = null,
    @Optional
    val botName: String? = null
) {
    @Transient
    private val botConfig: BotConfig by lazy {
        commonBot ?: botToken ?.let { token ->
            clientConfig ?.let { _ ->
                BotConfig(
                    token,
                    clientConfig
                )
            }
        } ?: throw IllegalStateException("You must set up \"commonBot\" or \"botToken\" field (remember that \"botToken\" is deprecated and will be replaced in future)")
    }

    @Transient
    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId.toChatId(),
            sourceChatId.toChatId(),
            (logsChatId ?: sourceChatId).toChatId(),
            botConfig.createBot(),
            databaseConfig,
            plugins,
            botName
        )
}

class FinalConfig (
    val targetChatId: ChatId,
    val sourceChatId: ChatId,
    val logsChatId: ChatId,
    val bot: RequestsExecutor,
    val databaseConfig: DatabaseConfig,
    val pluginsConfigs: List<Plugin> = emptyList(),
    val botName: String?
)
