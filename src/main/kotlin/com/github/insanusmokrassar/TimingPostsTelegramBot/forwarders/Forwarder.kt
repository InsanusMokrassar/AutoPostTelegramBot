package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message

interface Forwarder : (TelegramBot, Long, Collection<Message>) -> Unit {
    fun canForward(message: Message): Boolean
    fun forward(
        bot: TelegramBot,
        targetChatId: Long,
        vararg messages: Message
    )

    override fun invoke(p1: TelegramBot, p2: Long, p3: Collection<Message>) = forward(p1, p2, *p3.toTypedArray())
}