package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
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
        bot.execute(GetChat(config.sourceChatId)).extractChat()
        bot.execute(GetChat(config.targetChatId)).extractChat()

        val pluginManager = DefaultPluginManager(
            config.pluginsConfigs
        )

        pluginManager.onInit(
            bot,
            config
        )

        coroutineScope {
            allMessagesListener.openSubscription().also {
                launch {
                    for (message in it) {
                        if (message.data.chat.id == config.sourceChatId) {
                            messagesListener.send(message)
                        }
                    }
                }
            }

            allMessagesListener.openSubscription().also {
                launch {
                    var mediaGroudId: MediaGroupIdentifier? = null
                    val mediaGroup: MutableList<Update<Message>> = mutableListOf()
                    suspend fun pushData() {
                        allMediaGroupsListener.send(
                            ArrayList(mediaGroup)
                        )
                        mediaGroup.clear()
                        mediaGroudId = null
                    }
                    for (message in it) {
                        val data = message.data
                        when (data) {
                            is MediaGroupMessage -> {
                                if (data.mediaGroupId != mediaGroudId) {
                                    pushData()
                                    mediaGroudId = data.mediaGroupId
                                }
                                mediaGroup.add(message)
                            }
                            else -> if (mediaGroup.isNotEmpty()) {
                                pushData()
                            }
                        }
                    }
                }
            }

            allCallbackQueryListener.openSubscription().also {
                launch {
                    for (queryUpdate in it) {
                        (queryUpdate.data as? MessageDataCallbackQuery) ?.also {
                            if (it.message.chat.id == config.sourceChatId) {
                                callbackQueryListener.send(queryUpdate)
                            }
                        }
                    }
                }
            }
            allMediaGroupsListener.openSubscription().also {
                launch {
                    for (mediaGroup in it) {
                        val mediaGroupChatId = mediaGroup.firstOrNull() ?.data ?.chat ?.id ?: continue
                        if (mediaGroupChatId == config.sourceChatId) {
                            mediaGroupsListener.send(mediaGroup)
                        }
                    }
                }
            }
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
        }
    )
}
