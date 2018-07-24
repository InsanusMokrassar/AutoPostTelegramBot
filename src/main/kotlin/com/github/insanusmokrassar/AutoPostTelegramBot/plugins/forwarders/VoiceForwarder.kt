package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendVoice
import java.io.IOException

class VoiceForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. voice() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            val message = postMessage.message ?: return@mapNotNull null
            postMessage to SendVoice(
                targetChatId,
                message.voice().fileId()
            ).apply {
                message.caption() ?.let {
                    caption(it)
                }
                message.voice().also {
                    it.duration() ?.let {
                        duration(it)
                    }
                }
            }
        }.map {
            pair->
            bot.execute(pair.second).let {
                response ->
                response.message() ?.let {
                    pair.first to it
                } ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }.toMap()
    }
}
