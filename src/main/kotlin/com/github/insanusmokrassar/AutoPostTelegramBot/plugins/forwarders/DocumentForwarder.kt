package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.textOrCaptionToMarkdown
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendDocument
import java.io.IOException

class DocumentForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. document() != null
    }

    override suspend fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            postMessage.message ?.let {
                postMessage to SendDocument(
                    targetChatId,
                    it.document().fileId()
                ).apply {
                    it.textOrCaptionToMarkdown() ?.also {
                        caption ->
                        caption(caption)
                        parseMode(
                            ParseMode.Markdown
                        )
                    }
                    it.document().fileName() ?.let {
                        fileName(it)
                    }
                }.parseMode(
                    ParseMode.Markdown
                )
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