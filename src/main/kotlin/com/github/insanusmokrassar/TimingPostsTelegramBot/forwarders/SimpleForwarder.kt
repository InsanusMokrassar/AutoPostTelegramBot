package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.ForwardMessage

class SimpleForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return true
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
            it.message
        }.forEach {
            bot.execute(
                ForwardMessage(
                    targetChatId,
                    it.chat().id(),
                    it.messageId()
                )
            )
        }
    }
}