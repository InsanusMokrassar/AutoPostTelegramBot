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

val allMessagesListener = BroadcastChannel<Update<Message>>(Channel.CONFLATED)
val allCallbackQueryListener = BroadcastChannel<Update<CallbackQuery>>(Channel.CONFLATED)
val allMediaGroupsListener = BroadcastChannel<List<Update<Message>>>(Channel.CONFLATED)

val messagesListener = BroadcastChannel<Update<Message>>(Channel.CONFLATED)
val callbackQueryListener = BroadcastChannel<Update<CallbackQuery>>(Channel.CONFLATED)
val mediaGroupsListener = BroadcastChannel<List<Update<Message>>>(Channel.CONFLATED)

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

            var mediaGroudId: MediaGroupIdentifier? = null
            val mediaGroup: MutableList<Update<Message>> = mutableListOf()
            suspend fun pushData() {
                allMediaGroupsListener.send(
                    ArrayList(mediaGroup)
                )
                mediaGroup.clear()
                mediaGroudId = null
            }
            allMessagesListener.subscribe(
                scope = this
            ) {
                val data = it.data
                when (data) {
                    is MediaGroupMessage -> {
                        when (mediaGroudId) {
                            null,
                            data.mediaGroupId -> mediaGroup.add(it)
                            else -> {
                                pushData()
                                mediaGroup.add(it)
                            }
                        }
                    }
                    else -> if (mediaGroup.isNotEmpty()) {
                        pushData()
                    }
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
                    allMessagesListener.send(this)
                },
                channelPostCallback = {
                    allMessagesListener.send(this)
                },
                callbackQueryCallback = {
                    allCallbackQueryListener.send(this)
                },
                scope = this
            )
        }
    }
}
