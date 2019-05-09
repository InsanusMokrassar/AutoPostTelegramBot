package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.MediaGroupUpdates.MediaGroupUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

const val extraSmallBroadcastCapacity = 4
const val smallBroadcastCapacity = 8
const val mediumBroadcastCapacity = 16
const val largeBroadcastCapacity = 32
const val extraLargeBroadcastCapacity = 64

const val commonListenersCapacity = mediumBroadcastCapacity

@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("allMessagesFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.allMessagesFlow")
)
val allMessagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("allCallbackQueryFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.allCallbackQueryFlow")
)
val allCallbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("allMediaGroupsFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.allMediaGroupsFlow")
)
val allMediaGroupsListener = BroadcastChannel<MediaGroupUpdate>(Channel.CONFLATED)

@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("checkedMessagesFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.checkedMessagesFlow")
)
val messagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("checkedCallbackQueryFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.checkedCallbackQueryFlow")
)
val callbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
@Deprecated(
    "Will be fully replaced with flow near time",
    ReplaceWith("checkedMediaGroupsFlow", "com.github.insanusmokrassar.AutoPostTelegramBot.checkedMediaGroupsFlow")
)
val mediaGroupsListener = BroadcastChannel<MediaGroupUpdate>(Channel.CONFLATED)

val allMessagesFlow: Flow<BaseMessageUpdate> = allMessagesListener.asFlow()
val allCallbackQueryFlow: Flow<CallbackQueryUpdate> = allCallbackQueryListener.asFlow()
val allMediaGroupsFlow: Flow<MediaGroupUpdate> = allMediaGroupsListener.asFlow()

val checkedMessagesFlow: Flow<BaseMessageUpdate> = messagesListener.asFlow()
val checkedCallbackQueryFlow: Flow<CallbackQueryUpdate> = callbackQueryListener.asFlow()
val checkedMediaGroupsFlow: Flow<MediaGroupUpdate> = mediaGroupsListener.asFlow()

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

        NewDefaultCoroutineScope().apply {
            launch {
                allMessagesFlow.collect {
                    if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                        messagesListener.send(it)
                    }
                }
            }

            launch {
                allCallbackQueryFlow.collect {
                    (it.data as? MessageDataCallbackQuery) ?.also { query ->
                        if (query.message.chat.id == config.sourceChatId) {
                            callbackQueryListener.send(it)
                        }
                    }
                }
            }

            launch {
                allMediaGroupsFlow.collect { mediaGroup ->
                    val mediaGroupChatId = mediaGroup.data.first().chat.id
                    if (mediaGroupChatId == config.sourceChatId) {
                        mediaGroupsListener.send(mediaGroup)
                    }
                }
            }

            val filter = config.createFilter(
                allMessagesListener,
                allMessagesListener,
                allMediaGroupsListener,
                allCallbackQueryListener
            )

            config.subscribe(
                filter,
                this
            )
        }
    }
}
