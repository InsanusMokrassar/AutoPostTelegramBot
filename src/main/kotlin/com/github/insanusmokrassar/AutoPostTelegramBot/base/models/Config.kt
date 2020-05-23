package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.toChatId
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdatesFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import org.h2.Driver

@Serializable
class Config (
    val targetChatId: Long,
    val sourceChatId: Long,
    val logsChatId: Long? = null,
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    val clientConfig: HttpClientConfig? = null,
    val plugins: List<Plugin> = emptyList(),
    val commonBot: BotConfig? = null
) {
    private val botConfig: BotConfig by lazy {
        commonBot ?: throw IllegalStateException("You must set up \"commonBot\" field")
    }

    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId.toChatId(),
            sourceChatId.toChatId(),
            (logsChatId ?: sourceChatId).toChatId(),
            botConfig.createBot(),
            databaseConfig,
            plugins,
            botConfig.webhookConfig,
            botConfig.longPollingConfig()
        )
}

data class FinalConfig (
    val targetChatId: ChatId,
    val sourceChatId: ChatId,
    val logsChatId: ChatId,
    val bot: RequestsExecutor,
    val databaseConfig: DatabaseConfig,
    val pluginsConfigs: List<Plugin> = emptyList(),
    private val webhookConfig: WebhookConfig? = null,
    private val longPollingConfig: LongPollingConfig? = null,
    private val errorsVerbose: Boolean = false
) {
    val postsMessagesTable = PostsMessagesInfoTable(databaseConfig.database)
    val postsTable = PostsBaseInfoTable(databaseConfig.database, postsMessagesTable)

    init {
        CommonKnownPostsTransactions.updatePostsAndPostsMessagesTables(postsTable, postsMessagesTable)
    }

    suspend fun startGettingUpdates(filter: UpdatesFilter, scope: CoroutineScope = NewDefaultCoroutineScope(4)) {
        webhookConfig ?.apply {
            startWebhookServer(
                bot,
                filter,
                scope,
                if (errorsVerbose) {
                    {
                        bot.sendToLogger(it, "Long polling getting updates")
                    }
                } else {
                    null
                }
            )
        } ?: longPollingConfig ?.apply {
            startLongPollingListening(
                bot,
                filter,
                scope,
                if (errorsVerbose) {
                    {
                        bot.sendToLogger(it, "Long polling getting updates")
                    }
                } else {
                    null
                }
            )
        } ?: error("Webhooks or long polling way for updates retrieving must be used, but nothing configured")
    }
}
