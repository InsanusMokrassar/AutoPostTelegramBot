package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.ListSerializer
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.toChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.update.*
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.UpdatesFilter
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.startGettingOfUpdates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
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
    val commonBot: BotConfig? = null
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
            botConfig.webhookConfig
        )
}

data class FinalConfig (
    val targetChatId: ChatId,
    val sourceChatId: ChatId,
    val logsChatId: ChatId,
    val bot: RequestsExecutor,
    val databaseConfig: DatabaseConfig,
    val pluginsConfigs: List<Plugin> = emptyList(),
    private val webhookConfig: WebhookConfig?
) {
    fun createFilter(
        messagesChannel: SendChannel<MessageUpdate>,
        channelPostChannel: SendChannel<ChannelPostUpdate>,
        mediaGroupChannel: SendChannel<List<MediaGroupUpdate>>,
        callbackQueryChannel: SendChannel<CallbackQueryUpdate>
    ): UpdatesFilter = UpdatesFilter(
        {
            messagesChannel.send(it)
        },
        {
            mediaGroupChannel.send(it)
        },
        channelPostCallback = {
            channelPostChannel.send(it)
        },
        channelPostMediaGroupCallback = {
            mediaGroupChannel.send(it)
        },
        callbackQueryCallback = {
            callbackQueryChannel.send(it)
        },
        editedMessageMediaGroupCallback = null,
        editedChannelPostMediaGroupCallback = null
    )

    suspend fun subscribe(filter: UpdatesFilter, scope: CoroutineScope = NewDefaultCoroutineScope(4)) {
        webhookConfig ?.setWebhook(
            bot,
            filter,
            scope
        ) ?: bot.startGettingOfUpdates(
            scope = scope,
            allowedUpdates = filter.allowedUpdates,
            block = filter.asUpdateReceiver
        )
    }
}
