package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendContact

class ContactForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. contact() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            val contact = it.contact()
            SendContact(
                targetChatId,
                contact.phoneNumber(),
                contact.firstName()
            ).apply {
                contact.lastName() ?.let {
                    lastName(it)
                }
                contact.userId() ?.let {
                    parameters["user_id"] = it
                }
            }
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}