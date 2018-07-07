package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.BotIncomeMessagesListener
import com.github.insanusmokrassar.IObjectKRealisations.load
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.initChooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.PostPublisher
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initSubscription
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetChat
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetSocketAddress
import java.net.Proxy

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

    val startPost = StartPost()
    val fixPost = FixPost(bot)
    val mostRated = MostRated(bot)
    val deletePost = DeletePost(bot, config.logsChatId)
    val availableRates = AvailableRates(bot)

    val messagesListener = OnMessage(config, startPost, fixPost, mostRated, deletePost, availableRates)
    val mediaGroupsListener = OnMediaGroup(config, startPost, fixPost)
    val onCallbackQuery = OnCallbackQuery(bot)

    bot.setUpdatesListener(
        BotIncomeMessagesListener(
            messagesListener,
            onChannelPost = messagesListener,
            onCallbackQuery = onCallbackQuery,
            onMessageMediaGroup = mediaGroupsListener,
            onChannelPostMediaGroup = mediaGroupsListener
        )
    )

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
}
