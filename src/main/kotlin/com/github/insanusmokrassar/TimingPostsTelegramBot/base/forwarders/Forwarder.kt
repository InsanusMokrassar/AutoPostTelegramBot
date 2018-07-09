package com.github.insanusmokrassar.TimingPostsTelegramBot.base.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot

interface Forwarder {
    fun canForward(message: PostMessage): Boolean
    fun forward(
        bot: TelegramBot,
        targetChatId: Long,
        vararg messages: PostMessage
    ): List<Int>
}