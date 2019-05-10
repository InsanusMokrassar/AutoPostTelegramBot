package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.MediaGroupUpdates.MediaGroupUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.MessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.FlowsUpdatesFilter
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.UpdateReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.collect
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

val flowFilter = FlowsUpdatesFilter()

@Deprecated("Solved to use flows in the future. Use \"flowFilter\" instead")
val allMessagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
@Deprecated("Solved to use flows in the future. Use \"flowFilter\" instead")
val allCallbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
@Deprecated("Solved to use flows in the future. Use \"flowFilter\" instead")
val allMediaGroupsListener = BroadcastChannel<MediaGroupUpdate>(Channel.CONFLATED)

val messagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
val callbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
val mediaGroupsListener = BroadcastChannel<MediaGroupUpdate>(Channel.CONFLATED)

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

        NewDefaultCoroutineScope(8).apply {
            val messageUpdatesCollector: UpdateReceiver<BaseMessageUpdate> = {
                allMessagesListener.offer(it)
                if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                    messagesListener.send(it)
                }
            }
            launch {
                flowFilter.messageFlow.collect(messageUpdatesCollector)
            }
            launch {
                flowFilter.editedMessageFlow.collect(messageUpdatesCollector)
            }
            launch {
                flowFilter.channelPostFlow.collect(messageUpdatesCollector)
            }
            launch {
                flowFilter.editedChannelPostFlow.collect(messageUpdatesCollector)
            }

            launch {
                flowFilter.callbackQueryFlow.collect {
                    allCallbackQueryListener.offer(it)
                    (it.data as? MessageDataCallbackQuery) ?.also { query ->
                        if (query.message.chat.id == config.sourceChatId) {
                            callbackQueryListener.send(it)
                        }
                    }
                }
            }

            val mediaGroupUpdatesCollector: UpdateReceiver<MediaGroupUpdate> = { mediaGroup ->
                allMediaGroupsListener.offer(mediaGroup)
                val mediaGroupChatId = mediaGroup.data.first().chat.id
                if (mediaGroupChatId == config.sourceChatId) {
                    mediaGroupsListener.send(mediaGroup)
                }
            }
            launch {
                flowFilter.messageMediaGroupFlow.collect(mediaGroupUpdatesCollector)
            }
            launch {
                flowFilter.channelPostMediaGroupFlow.collect(mediaGroupUpdatesCollector)
            }

            config.subscribe(
                flowFilter.filter,
                this
            )
        }
    }
}
