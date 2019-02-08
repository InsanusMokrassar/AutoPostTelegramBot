package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactions.TransactionsController
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.startGettingOfUpdates
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class AutoPostTelegramBot(
    val config: FinalConfig,

    val allMessagesListener: BroadcastChannel<BaseMessageUpdate> = BroadcastChannel(commonListenersCapacity),
    val allCallbackQueryListener: BroadcastChannel<CallbackQueryUpdate> = BroadcastChannel(commonListenersCapacity),
    val allMediaGroupsListener: BroadcastChannel<List<BaseMessageUpdate>> = BroadcastChannel(commonListenersCapacity),

    val messagesListener: BroadcastChannel<BaseMessageUpdate> = BroadcastChannel(commonListenersCapacity),
    val callbackQueryListener: BroadcastChannel<CallbackQueryUpdate> = BroadcastChannel(commonListenersCapacity),
    val mediaGroupsListener: BroadcastChannel<List<BaseMessageUpdate>> = BroadcastChannel(commonListenersCapacity)
) {
    private val scope = NewDefaultCoroutineScope()
    val executor = config.bot

    val pluginManager = DefaultPluginManager(
        config.pluginsConfigs
    )

    val postsTable: PostsTable = PostsTable(config.botName ?.let { "${it}_PostsTable" } ?: "")
    val postsMessagesTable: PostsMessagesTable = PostsMessagesTable(config.botName ?.let { "${it}_PostsMessagesTable" } ?: "", postsTable)

    val transactionsController = TransactionsController(
        postsTable,
        postsMessagesTable
    )

    init {
        config.databaseConfig.apply {
            connect()

            transaction {
                SchemaUtils.createMissingTablesAndColumns(postsTable, postsMessagesTable)
            }
        }

        runBlocking {
            commonLogger.info("Source chat: ${executor.execute(GetChat(config.sourceChatId)).extractChat()}")
            commonLogger.info("Target chat: ${executor.execute(GetChat(config.targetChatId)).extractChat()}")

            pluginManager.onInit(this@AutoPostTelegramBot)
        }

        allMessagesListener.subscribe(
            scope = scope
        ) {
            if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                messagesListener.send(it)
            }
        }

        allCallbackQueryListener.subscribe(
            scope = scope
        ) {
            (it.data as? MessageDataCallbackQuery) ?.also { query ->
                if (query.message.chat.id == config.sourceChatId) {
                    callbackQueryListener.send(it)
                }
            }
        }
        allMediaGroupsListener.subscribe(
            scope = scope
        ) { mediaGroup ->
            val mediaGroupChatId = mediaGroup.firstOrNull() ?.data ?.chat ?.id ?: return@subscribe
            if (mediaGroupChatId == config.sourceChatId) {
                mediaGroupsListener.send(mediaGroup)
            }
        }

        executor.startGettingOfUpdates(
            messageCallback = {
                allMessagesListener.send(it)
            },
            mediaGroupCallback = {
                allMediaGroupsListener.send(it)
            },
            channelPostCallback = {
                allMessagesListener.send(it)
            },
            callbackQueryCallback = {
                allCallbackQueryListener.send(it)
            },
            scope = scope
        )
    }
}