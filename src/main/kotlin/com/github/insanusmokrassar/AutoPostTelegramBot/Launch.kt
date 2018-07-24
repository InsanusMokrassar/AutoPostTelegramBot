package com.github.insanusmokrassar.AutoPostTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.*
import com.github.insanusmokrassar.IObjectKRealisations.load
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.Config
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.DefaultPluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
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

// SUBSCRIBE WITH CAUTION
val realMessagesListener = UpdateCallbackChannel<Message>()
val realCallbackQueryListener = UpdateCallbackChannel<CallbackQuery>()
val realMediaGroupsListener = MediaGroupCallbackChannel()

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
            SchemaUtils.createMissingTablesAndColumns(PostsTable, PostsMessagesTable)
        }
    }

    if (!bot.execute(GetChat(config.sourceChatId)).isOk || !bot.execute(GetChat(config.targetChatId)).isOk) {
        throw IllegalArgumentException("Can't check chats availability")
    }

    val pluginManager = DefaultPluginManager(
        config.pluginsConfigs,
        *config.additionalClassLoaders
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
