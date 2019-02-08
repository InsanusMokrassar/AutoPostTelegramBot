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
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.startGettingOfUpdates
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.coroutineScope
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

val allMessagesListener = BroadcastChannel<BaseMessageUpdate>(commonListenersCapacity)
val allCallbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(commonListenersCapacity)
val allMediaGroupsListener = BroadcastChannel<List<BaseMessageUpdate>>(commonListenersCapacity)

val messagesListener = BroadcastChannel<BaseMessageUpdate>(commonListenersCapacity)
val callbackQueryListener = BroadcastChannel<CallbackQueryUpdate>(commonListenersCapacity)
val mediaGroupsListener = BroadcastChannel<List<BaseMessageUpdate>>(commonListenersCapacity)

fun main(args: Array<String>) {
    val bot = AutoPostTelegramBot(
        load(args[0], Config.serializer()).finalConfig,
        allMessagesListener,
        allCallbackQueryListener,
        allMediaGroupsListener,
        messagesListener,
        callbackQueryListener,
        mediaGroupsListener
    )
}
