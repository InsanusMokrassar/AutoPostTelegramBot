package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.BotIncomeMessagesListener.BotIncomeMessagesListener
import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetChat

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
    var willHaveMessages = false

    bot.execute(GetChat(config.sourceChatId)).let {
        willHaveMessages = it.chat() ?.type() ?.let {
            it != Chat.Type.channel
        } ?: false
    }

    bot.execute(GetChat(config.targetChatId)).let {
        willHaveMessages = it.chat() ?.type() ?.let {
            it != Chat.Type.channel
        } ?: false
    }


}
