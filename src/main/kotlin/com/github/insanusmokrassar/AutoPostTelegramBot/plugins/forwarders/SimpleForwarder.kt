package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.ForwardMessage

class SimpleForwarder : Forwarder {

    override val importance: Int = LOW_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return true
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            ForwardMessage(
                targetChatId,
                it.chat().id(),
                it.messageId()
            )
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}