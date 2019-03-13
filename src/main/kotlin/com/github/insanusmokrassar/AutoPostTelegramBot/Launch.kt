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
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
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

        NewDefaultCoroutineScope().apply {
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
