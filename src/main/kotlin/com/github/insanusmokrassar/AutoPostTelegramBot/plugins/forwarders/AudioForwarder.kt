package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendAudio
import java.io.IOException

class AudioForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. audio() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendAudio(
                targetChatId,
                it.audio().fileId()
            ).apply {
                it.caption() ?.let {
                    caption(it)
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