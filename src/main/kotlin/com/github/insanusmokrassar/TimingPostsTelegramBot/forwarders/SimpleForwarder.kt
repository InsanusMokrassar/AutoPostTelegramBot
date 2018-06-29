package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.ForwardMessage

class SimpleForwarder : Forwarder {
    override fun canForward(message: Message): Boolean {
        return true
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: Message) {
        messages.forEach {
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