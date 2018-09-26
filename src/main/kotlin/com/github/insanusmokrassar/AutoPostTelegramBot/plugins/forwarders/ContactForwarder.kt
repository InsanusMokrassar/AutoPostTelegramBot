package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendContact
import java.io.IOException

class ContactForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. contact() != null
    }

    override suspend fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            postMessage.message ?.let {
                val contact = it.contact()
                postMessage to
                SendContact(
                    targetChatId,
                    contact.phoneNumber(),
                    contact.firstName()
                ).apply {
                    contact.lastName()?.let {
                        lastName(it)
                    }
                    contact.userId()?.let {
                        parameters["user_id"] = it
                    }
                }
            }
        }.map {
            (original, request) ->
            bot.executeBlocking(request).let {
                response ->
                response.message() ?.let {
                    original to it
                } ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }.toMap()
    }
}