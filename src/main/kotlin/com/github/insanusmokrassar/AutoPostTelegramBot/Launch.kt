package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.BotIncomeMessagesListener.*
import com.github.insanusmokrassar.IObjectKRealisations.load
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetChat
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

// SUBSCRIBE WITH CAUTION
val realMessagesListener = UpdateCallbackChannel<Message>()
val realCallbackQueryListener = UpdateCallbackChannel<CallbackQuery>()
val realMediaGroupsListener = MediaGroupCallbackChannel()

val messagesListener = BroadcastChannel<Pair<Int, Message>>(Channel.CONFLATED)
val callbackQueryListener = BroadcastChannel<Pair<Int, CallbackQuery>>(Channel.CONFLATED)
val mediaGroupsListener = BroadcastChannel<Pair<String, List<Message>>>(Channel.CONFLATED)

fun main(args: Array<String>) {
    val config = load(args[0]).toObject(Config::class.java).finalConfig

    val bot = TelegramBot.Builder(
        config.botToken
    ).run {
        if (config.debug) {
            debug()
        }
        config.clientConfig ?.also {
            okHttpClient(it.createClient())
        }
        build()
    }

    config.databaseConfig.apply {
        Database.connect(
            url,
            driver,
            username,
            password
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(PostsTable, PostsMessagesTable)
        }
    }

    config.regen ?.applyFor(bot)

    runBlocking {
        if (!bot.executeBlocking(GetChat(config.sourceChatId)).isOk || !bot.executeBlocking(GetChat(config.targetChatId)).isOk) {
            throw IllegalArgumentException("Can't check chats availability")
        }
    }

    val pluginManager = DefaultPluginManager(
        config.pluginsConfigs
    )

    pluginManager.onInit(
        bot,
        config
    )

    realMessagesListener.broadcastChannel.subscribe {
        if (it.second.chat().id() == config.sourceChatId) {
            messagesListener.send(it)
        }
    }

    realCallbackQueryListener.broadcastChannel.subscribe {
        if (it.second.message().chat().id() == config.sourceChatId) {
            callbackQueryListener.send(it)
        }
    }

    realMediaGroupsListener.broadcastChannel.subscribe {
        if (it.second.firstOrNull { it.chat().id() != config.sourceChatId } == null) {
            mediaGroupsListener.send(it)
        }
    }

    bot.setUpdatesListener(
        BotIncomeMessagesListener(
            realMessagesListener,
            onChannelPost = realMessagesListener,
            onCallbackQuery = realCallbackQueryListener,
            onMessageMediaGroup = realMediaGroupsListener,
            onChannelPostMediaGroup = realMediaGroupsListener
        )
    )
}
