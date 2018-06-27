package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.BotIncomeMessagesListener
import com.github.insanusmokrassar.IObjectKRealisations.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks.OnMediaGroup
import com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks.OnMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
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

    val messagesListener = OnMessage(config)
    val mediaGroupsListener = OnMediaGroup(config)

    bot.setUpdatesListener(
        BotIncomeMessagesListener(
            messagesListener,
            onChannelPost = messagesListener,
            onMessageMediaGroup = mediaGroupsListener,
            onChannelPostMediaGroup = mediaGroupsListener
        )
    )
}
