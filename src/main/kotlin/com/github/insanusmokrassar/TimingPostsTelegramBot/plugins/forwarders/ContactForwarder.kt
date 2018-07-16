package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendContact

class ContactForwarder : Forwarder {
    override val version: PluginVersion = 0L
    override val importance: Int = MIDDLE_PRIORITY

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