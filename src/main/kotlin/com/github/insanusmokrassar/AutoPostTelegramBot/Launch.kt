package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.combineFlows
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.load
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.filterBaseMessageUpdates
import com.github.insanusmokrassar.TelegramBotAPI.requests.chat.get.GetChat
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.mediaCountInMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.MediaGroupUpdates.*
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.updateshandlers.FlowsUpdatesFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val extraSmallBroadcastCapacity = 4
const val smallBroadcastCapacity = 8
const val mediumBroadcastCapacity = 16
const val largeBroadcastCapacity = 32
const val extraLargeBroadcastCapacity = 64

const val commonListenersCapacity = mediumBroadcastCapacity

private val scope = CoroutineScope(Dispatchers.Default)

val flowFilter = FlowsUpdatesFilter(extraLargeBroadcastCapacity)

lateinit var checkedMessagesFlow: Flow<BaseSentMessageUpdate>
    private set
lateinit var checkedEditedMessagesFlow: Flow<BaseEditMessageUpdate>
    private set
lateinit var checkedCallbacksQueriesFlow: Flow<CallbackQueryUpdate>
    private set
lateinit var checkedMediaGroupsFlow: Flow<SentMediaGroupUpdate>
    private set

fun main(args: Array<String>) {
    val config: FinalConfig = load(args[0], Config.serializer()).finalConfig

    checkedMessagesFlow = combineFlows(flowFilter.messageFlow, flowFilter.channelPostFlow, scope = scope).filterBaseMessageUpdates(
        config.sourceChatId
    ).filter { it.data !is MediaGroupMessage }
    checkedEditedMessagesFlow = combineFlows(flowFilter.editedMessageFlow, flowFilter.editedChannelPostFlow, scope = scope).filterBaseMessageUpdates(
        config.sourceChatId
    ).filter { it.data !is MediaGroupMessage }
    checkedCallbacksQueriesFlow = flowFilter.callbackQueryFlow.filter {
        (it.data as? MessageDataCallbackQuery) ?.let { query ->
            query.message.chat.id == config.sourceChatId
        } ?: false
    }
    val mediaGroupsChannel = Channel<Pair<SentMediaGroupUpdate, List<BaseMessageUpdate>>>()

    scope.launch {
        val currentList = mutableListOf<SentMediaGroupUpdate>()
        val mutex = Mutex()
        suspend fun send() {
            mediaGroupsChannel.send(currentList.first() to currentList.flatMap { it.origins })
            currentList.clear()
        }
        var debounceJob: Job? = null
        launch {
            combineFlows(flowFilter.messageMediaGroupFlow, flowFilter.channelPostMediaGroupFlow, scope = scope).filter {
                val mediaGroupChatId = it.data.first().chat.id
                mediaGroupChatId == config.sourceChatId
            }.collectWithErrors({ _, _ -> if (mutex.isLocked) mutex.unlock() }) {
                debounceJob ?.cancelAndJoin()
                mutex.withLock {
                    val currentMediaGroup = it.data.first().mediaGroupId
                    val previousMediaGroup = currentList.firstOrNull() ?.data ?.first() ?.mediaGroupId
                    if (currentMediaGroup != previousMediaGroup && currentList.isNotEmpty()) {
                        commonLogger.info("Current mediagroup do not equals to previous ($currentMediaGroup, $previousMediaGroup)")
                        send()
                    }
                    currentList.add(it)
                    if (currentList.size == mediaCountInMediaGroup.last) {
                        send()
                    } else {
                        debounceJob = scope.launch {
                            safely({ if (mutex.isLocked) mutex.unlock() }) {
                                delay(5000L)
                                mutex.withLock {
                                    commonLogger.info("Sending after debounce")
                                    if (currentList.isNotEmpty()) {
                                        send()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        launch {
            checkedMessagesFlow.collectWithErrors {
                debounceJob ?.cancelAndJoin()
                mutex.withLock {
                    if (currentList.isNotEmpty()) {
                        commonLogger.info("Sending on non-mediagroup message")
                        send()
                    }
                }
            }
        }
    }
    checkedMediaGroupsFlow = mediaGroupsChannel.consumeAsFlow().mapNotNull { (update, baseUpdates) ->
        when (update) {
            is ChannelPostMediaGroupUpdate -> ChannelPostMediaGroupUpdate(baseUpdates)
            is MessageMediaGroupUpdate -> MessageMediaGroupUpdate(baseUpdates)
            else -> null
        }
    }

    val bot = config.bot

    config.also {
        PostsTable = it.postsTable
        PostsMessagesTable = it.postsMessagesTable
    }

    scope.launch {
        commonLogger.info("Source chat: ${bot.execute(GetChat(config.sourceChatId))}")
        commonLogger.info("Target chat: ${bot.execute(GetChat(config.targetChatId))}")

        val pluginManager = DefaultPluginManager(
            config.pluginsConfigs
        )

        pluginManager.onInit(
            bot,
            config
        )

        config.startGettingUpdates(
            flowFilter,
            scope
        )
    }
    runBlocking {
        scope.coroutineContext[Job]!!.join()
    }
}
