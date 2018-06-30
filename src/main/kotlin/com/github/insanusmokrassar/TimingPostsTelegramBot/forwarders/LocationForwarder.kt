package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendLocation

class LocationForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. location() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
            it.message
        }.map {
            SendLocation(
                targetChatId,
                it.location().latitude(),
                it.location().longitude()
            )
        }.forEach {
            bot.execute(it)
        }
    }
}