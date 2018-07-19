package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.io.IOException

class TextForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. text() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendMessage(
                targetChatId,
                it.text()
            ).parseMode(
                ParseMode.Markdown
            )
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