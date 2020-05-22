package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.MediaGroupUpdates.SentMediaGroupUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.FlowsUpdatesFilter
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.UpdateReceiver
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val extraSmallBroadcastCapacity = 4
const val smallBroadcastCapacity = 8
const val mediumBroadcastCapacity = 16
const val largeBroadcastCapacity = 32
const val extraLargeBroadcastCapacity = 64

const val commonListenersCapacity = mediumBroadcastCapacity

val flowFilter = FlowsUpdatesFilter()

private val messagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
private val editedMessagesListener = BroadcastChannel<BaseMessageUpdate>(Channel.CONFLATED)
private val callbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(Channel.CONFLATED)
private val mediaGroupsListener = BroadcastChannel<SentMediaGroupUpdate>(Channel.CONFLATED)

val checkedMessagesFlow = messagesListener.asFlow()
val checkedEditedMessagesFlow = editedMessagesListener.asFlow()
val checkedCallbacksQueriesFlow = callbackQueryListener.asFlow()
val checkedMediaGroupsFlow = mediaGroupsListener.asFlow()

fun main(args: Array<String>) {
    val config: FinalConfig = load(args[0], Config.serializer()).finalConfig

    val bot = config.bot

    config.also {
        PostsTable = it.postsTable
        PostsMessagesTable = it.postsMessagesTable
    }

    runBlocking {
        commonLogger.info("Source chat: ${bot.execute(GetChat(config.sourceChatId))}")
        commonLogger.info("Target chat: ${bot.execute(GetChat(config.targetChatId))}")

        val pluginManager = DefaultPluginManager(
            config.pluginsConfigs
        )

        pluginManager.onInit(
            bot,
            config
        )

        NewDefaultCoroutineScope(8).apply {
            val messageUpdatesCollector: UpdateReceiver<BaseMessageUpdate> = {
                if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                    messagesListener.send(it)
                }
            }
            val editMessageUpdatesCollector: UpdateReceiver<BaseMessageUpdate> = {
                if (it.data.chat.id == config.sourceChatId && it.data !is MediaGroupMessage) {
                    editedMessagesListener.send(it)
                }
            }
            launch {
                flowFilter.messageFlow.collectWithErrors(messageUpdatesCollector)
            }
            launch {
                flowFilter.editedMessageFlow.collectWithErrors(editMessageUpdatesCollector)
            }
            launch {
                flowFilter.channelPostFlow.collectWithErrors(messageUpdatesCollector)
            }
            launch {
                flowFilter.editedChannelPostFlow.collectWithErrors(editMessageUpdatesCollector)
            }

            launch {
                flowFilter.callbackQueryFlow.collectWithErrors {
                    (it.data as? MessageDataCallbackQuery) ?.also { query ->
                        if (query.message.chat.id == config.sourceChatId) {
                            callbackQueryListener.send(it)
                        }
                    }
                }
            }

            val mediaGroupUpdatesCollector: UpdateReceiver<SentMediaGroupUpdate> = { mediaGroup ->
                val mediaGroupChatId = mediaGroup.data.first().chat.id
                if (mediaGroupChatId == config.sourceChatId) {
                    mediaGroupsListener.send(mediaGroup)
                }
            }
            launch {
                flowFilter.messageMediaGroupFlow.collectWithErrors(mediaGroupUpdatesCollector)
            }
            launch {
                flowFilter.channelPostMediaGroupFlow.collectWithErrors(mediaGroupUpdatesCollector)
            }

            config.subscribe(
                flowFilter.filter,
                this
            )
        }
    }
}
