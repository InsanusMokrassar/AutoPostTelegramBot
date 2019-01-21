package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.CallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.MediaGroupIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.startGettingOfUpdates
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@Deprecated("Old naming of vairable", ReplaceWith("allMessagesListener"))
val realMessagesListener
    get() = allMessagesListener
@Deprecated("Old naming of vairable", ReplaceWith("allCallbackQueryListener"))
val realCallbackQueryListener
    get() = allCallbackQueryListener
@Deprecated("Old naming of vairable", ReplaceWith("allMediaGroupsListener"))
val realMediaGroupsListener
    get() = allMediaGroupsListener

val allMessagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
val allCallbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
val allMediaGroupsListener = BroadcastChannel<List<BaseMessageUpdate>>(Channel.CONFLATED)

val messagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
val callbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
val mediaGroupsListener = BroadcastChannel<List<BaseMessageUpdate>>(Channel.CONFLATED)

fun main(args: Array<String>) {
    val config: FinalConfig = load(args[0], Config.serializer()).finalConfig

    val bot = config.bot

    config.databaseConfig.apply {
        connect()

        transaction {
            SchemaUtils.createMissingTablesAndColumns(PostsTable, PostsMessagesTable)
        }
    }

    runBlocking {
        commonLogger.info("Source chat: ${bot.execute(GetChat(config.sourceChatId)).extractChat()}")
        commonLogger.info("Target chat: ${bot.execute(GetChat(config.targetChatId)).extractChat()}")

        val pluginManager = DefaultPluginManager(
            config.pluginsConfigs
        )

        pluginManager.onInit(
            bot,
            config
        )

        coroutineScope {
            allMessagesListener.subscribe(
                scope = this
            ) {
                if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                    messagesListener.send(it)
                }
            }

            allCallbackQueryListener.subscribe(
                scope = this
            ) {
                (it.data as? MessageDataCallbackQuery) ?.also { query ->
                    if (query.message.chat.id == config.sourceChatId) {
                        callbackQueryListener.send(it)
                    }
                }
            }
            allMediaGroupsListener.subscribe(
                scope = this
            ) { mediaGroup ->
                val mediaGroupChatId = mediaGroup.firstOrNull() ?.data ?.chat ?.id ?: return@subscribe
                if (mediaGroupChatId == config.sourceChatId) {
                    mediaGroupsListener.send(mediaGroup)
                }
            }

            bot.startGettingOfUpdates(
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
                scope = this
            )
        }
    }
}
