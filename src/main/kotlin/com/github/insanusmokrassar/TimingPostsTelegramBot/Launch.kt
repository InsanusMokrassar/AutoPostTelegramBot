package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.*
import com.github.insanusmokrassar.IObjectKRealisations.load
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.initChooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.PostPublisher
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initSubscription
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetChat
import kotlinx.coroutines.experimental.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetSocketAddress
import java.net.Proxy

val messagesListener = UpdateCallbackChannel<Message>()
val callbackQueryListener = UpdateCallbackChannel<CallbackQuery>()
val mediaGroupsListener = MediaGroupCallbackChannel()

fun main(args: Array<String>) {
    val config = load(args[0]).toObject(Config::class.java).finalConfig

    val bot = TelegramBot.Builder(
        config.botToken
    ).run {
        if (config.debug) {
            debug()
        }
        config.proxy ?.let {
            proxy ->
            okHttpClient(
                OkHttpClient.Builder().apply {
                    proxy(
                        Proxy(
                            Proxy.Type.SOCKS,
                            InetSocketAddress(
                                proxy.host,
                                proxy.port
                            )
                        )
                    )
                    proxy.password ?.let {
                        password ->
                        proxyAuthenticator {
                            _, response ->
                            response.request().newBuilder().apply {
                                addHeader(
                                    "Proxy-Authorization",
                                    Credentials.basic(proxy.username ?: "", password)
                                )
                            }.build()
                        }
                    }
                }.build()
            )
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
            SchemaUtils.createMissingTablesAndColumns(PostsTable, PostsLikesTable, PostsMessagesTable)
        }
    }

    if (!bot.execute(GetChat(config.sourceChatId)).isOk || !bot.execute(GetChat(config.targetChatId)).isOk) {
        throw IllegalArgumentException("Can't check chats availability")
    }

    callbackQueryListener.broadcastChannel.openSubscription().also {
        val listener = OnCallbackQuery(bot)
        launch {
            while (isActive) {
                val received = it.receive()
                listener.invoke(received.first, received.second)
            }
            it.cancel()
        }
    }

    mediaGroupsListener.broadcastChannel.openSubscription().also {
        val listener = OnMediaGroup(config)
        launch {
            while (isActive) {
                val received = it.receive()
                listener.invoke(received.first, received.second)
            }
            it.cancel()
        }
    }

    val chooser = initChooser(
        config.chooser.name,
        config.chooser.params
    )

    val publisher = PostPublisher(
        config.targetChatId,
        config.sourceChatId,
        bot,
        config.logsChatId,
        listOf(
            PhotoForwarder(),
            VideoForwarder(),
            MediaGroupForwarder(),
            AudioForwarder(),
            VoiceForwarder(),
            DocumentForwarder(),
            TextForwarder(),
            LocationForwarder(),
            ContactForwarder(),
            SimpleForwarder()
        )
    )

    initSubscription(
        config.sourceChatId,
        bot
    )

    config.plugins.forEach {
        it.init(
            config,
            chooser,
            publisher,
            bot
        )
    }

    bot.setUpdatesListener(
        BotIncomeMessagesListener(
            messagesListener,
            onChannelPost = messagesListener,
            onCallbackQuery = callbackQueryListener,
            onMessageMediaGroup = mediaGroupsListener,
            onChannelPostMediaGroup = mediaGroupsListener
        )
    )
}
