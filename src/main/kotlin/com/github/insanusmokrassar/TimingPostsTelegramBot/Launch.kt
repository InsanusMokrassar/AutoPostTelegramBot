package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.BotIncomeMessagesListener
import com.github.insanusmokrassar.IObjectKRealisations.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.PostingStrategies.TimerStrategy
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetChat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    val config = load(args[0]).toObject(Config::class.java).finalConfig

    val bot = TelegramBot.Builder(
        config.botToken
    ).run {
        if (config.debug) {
            debug()
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
            SchemaUtils.createMissingTablesAndColumns(PostsLikesTable, PostsMessagesTable, PostsTable)
        }
    }

    if (!bot.execute(GetChat(config.sourceChatId)).isOk || !bot.execute(GetChat(config.targetChatId)).isOk) {
        throw IllegalArgumentException("Can't check chats availability")
    }

    val startPost = StartPost()
    val fixPost = FixPost(bot)
    val mostRated = MostRated(bot)

    val messagesListener = OnMessage(config, startPost, fixPost, mostRated)
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

    TimerStrategy(
        config.targetChatId.toLong(),
        config.sourceChatId.toLong(),
        bot,
        listOf(
            PhotoForwarder(),
            VideoForwarder(),
            MediaGroupForwarder(),
            AudioForwarder(),
            DocumentForwarder(),
            TextForwarder(),
            SimpleForwarder()
        ),
        config.postDelay
    )
}
