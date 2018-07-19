package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendContact
import java.io.IOException

class ContactForwarder : Forwarder {

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
        }.map {
            bot.execute(it).let {
                response ->
                response.message() ?.messageId() ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }
    }
}