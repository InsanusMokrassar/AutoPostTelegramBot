package dev.inmo.AutoPostTelegramBot.base.models

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.CommonKnownPostsTransactions
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.AutoPostTelegramBot.utils.extensions.sendToLogger
import dev.inmo.micro_utils.coroutines.ExceptionHandler
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.updateshandlers.UpdatesFilter
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

    private val exceptionHandler: ExceptionHandler<Unit>? = if (errorsVerbose) {
        { bot.sendToLogger(it, "Long polling getting updates") }
    } else {
        null
    }

    suspend fun startGettingUpdates(filter: UpdatesFilter, scope: CoroutineScope = NewDefaultCoroutineScope(4)) {
        webhookConfig ?.apply {
            startWebhookServer(
                bot,
                filter,
                scope,
                exceptionHandler
            )
        } ?: longPollingConfig ?.apply {
            startLongPollingListening(
                bot,
                filter,
                scope,
                exceptionHandler
            )
        } ?: error("Webhooks or long polling way for updates retrieving must be used, but nothing configured")
    }
}
