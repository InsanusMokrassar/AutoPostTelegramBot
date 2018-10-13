package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeBlocking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.textOrCaptionToMarkdown
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendAudio
import java.io.IOException

class AudioForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. audio() != null
    }

    override suspend fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            postMessage.message ?.let {
                postMessage to SendAudio(
                    targetChatId,
                    it.audio().fileId()
                ).apply {
                    it.textOrCaptionToMarkdown() ?.also {
                        caption ->
                        caption(caption)
                        parseMode(
                            ParseMode.Markdown
                        )
                    }
                    it.audio().also {
                        it.title() ?.let {
                            title(it)
                        }
                        it.duration() ?.let {
                            duration(it)
                        }
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