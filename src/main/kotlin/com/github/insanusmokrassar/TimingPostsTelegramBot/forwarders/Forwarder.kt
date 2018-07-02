package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot

interface Forwarder {
    fun canForward(message: PostMessage): Boolean
    fun forward(
        bot: TelegramBot,
        targetChatId: Long,
        vararg messages: PostMessage
    ): List<Int>
}