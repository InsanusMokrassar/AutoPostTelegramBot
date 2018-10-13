package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.textOrCaptionToMarkdown
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.io.IOException

class TextForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. text() != null
    }

    override suspend fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            val message = postMessage.message ?: return@mapNotNull null
            postMessage to SendMessage(
                targetChatId,
                message.textOrCaptionToMarkdown()
            ).parseMode(
                ParseMode.Markdown
            )
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