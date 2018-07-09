package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.*
import com.github.insanusmokrassar.IObjectKRealisations.load
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.initChooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.Config
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.PostPublisher
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initSubscription
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetChat
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetSocketAddress
import java.net.Proxy


private val realMessagesListener = UpdateCallbackChannel<Message>()
private val realCallbackQueryListener = UpdateCallbackChannel<CallbackQuery>()
private val realMediaGroupsListener = MediaGroupCallbackChannel()

private const val subscriptionsCount = 256

val messagesListener = BroadcastChannel<Pair<Int, Message>>(subscriptionsCount)
val callbackQueryListener = BroadcastChannel<Pair<Int, CallbackQuery>>(subscriptionsCount)
val mediaGroupsListener = BroadcastChannel<Pair<String, List<Message>>>(subscriptionsCount)

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

    val chooser = initChooser(
        config.chooser.name,
        config.chooser.params
    )

    val publisher = PostPublisher(
        config,
        bot,
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

    realMessagesListener.broadcastChannel.openSubscription().also {
        launch {
            while (isActive) {
                val received = it.receive()
                try {
                    if (received.second.chat().id() == config.sourceChatId) {
                        messagesListener.send(received)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            it.cancel()
        }
    }

    realCallbackQueryListener.broadcastChannel.openSubscription().also {
        launch {
            while (isActive) {
                val received = it.receive()
                try {
                    if (received.second.message().chat().id() == config.sourceChatId) {
                        callbackQueryListener.send(received)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            it.cancel()
        }
    }

    realMediaGroupsListener.broadcastChannel.openSubscription().also {
        launch {
            while (isActive) {
                val received = it.receive()
                try {
                    if (received.second.firstOrNull { it.chat().id() != config.sourceChatId } == null) {
                        mediaGroupsListener.send(received)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            it.cancel()
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
