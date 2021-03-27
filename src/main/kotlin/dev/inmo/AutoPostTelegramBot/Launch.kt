package dev.inmo.AutoPostTelegramBot

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsTable
import dev.inmo.AutoPostTelegramBot.base.models.Config
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import dev.inmo.AutoPostTelegramBot.base.plugins.commonLogger
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.AutoPostTelegramBot.utils.flow.combineFlows
import dev.inmo.AutoPostTelegramBot.utils.load
import dev.inmo.micro_utils.coroutines.*
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.utils.shortcuts.mediaGroupId
import dev.inmo.tgbotapi.extensions.utils.updates.*
import dev.inmo.tgbotapi.requests.chat.get.GetChat
import dev.inmo.tgbotapi.types.CallbackQuery.MessageDataCallbackQuery
import dev.inmo.tgbotapi.types.MediaGroupIdentifier
import dev.inmo.tgbotapi.types.mediaCountInMediaGroup
import dev.inmo.tgbotapi.types.message.abstracts.MediaGroupMessage
import dev.inmo.tgbotapi.types.update.CallbackQueryUpdate
import dev.inmo.tgbotapi.types.update.MediaGroupUpdates.*
import dev.inmo.tgbotapi.types.update.abstracts.*
import dev.inmo.tgbotapi.updateshandlers.FlowsUpdatesFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.logging.Level

const val extraSmallBroadcastCapacity = 4
const val smallBroadcastCapacity = 8
const val mediumBroadcastCapacity = 16
const val largeBroadcastCapacity = 32
const val extraLargeBroadcastCapacity = 64

const val commonListenersCapacity = mediumBroadcastCapacity

private val scope = CoroutineScope(Dispatchers.Default)

val flowFilter = FlowsUpdatesFilter()

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

    checkedMessagesFlow = (flowFilter.messageFlow + flowFilter.channelPostFlow).filterBaseMessageUpdatesByChatId(
        config.sourceChatId
    ).filter { it.data !is MediaGroupMessage<*> }
    checkedEditedMessagesFlow = (flowFilter.editedMessageFlow + flowFilter.editedChannelPostFlow).filterBaseMessageUpdatesByChatId(
        config.sourceChatId
    ).filter { it.data !is MediaGroupMessage<*> }
    checkedCallbacksQueriesFlow = flowFilter.callbackQueryFlow.filter {
        (it.data as? MessageDataCallbackQuery) ?.let { query ->
            query.message.chat.id == config.sourceChatId
        } ?: false
    }
    checkedMediaGroupsFlow = (flowFilter.channelPostMediaGroupFlow + flowFilter.messageMediaGroupFlow).filterSentMediaGroupUpdatesByChatId(
        config.sourceChatId
    )

    val bot = config.bot

    config.also {
        PostsTable = it.postsTable
        PostsMessagesTable = it.postsMessagesTable
    }

    scope.launch {
        commonLogger.info("Source chat: ${bot.getChat(config.sourceChatId)}")
        commonLogger.info("Target chat: ${bot.getChat(config.targetChatId)}")

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
        scope.coroutineContext.job.join()
    }
}
