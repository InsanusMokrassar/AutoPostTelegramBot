package com.github.insanusmokrassar.TimingPostsTelegramBot.base.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendVoice

class VoiceForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. voice() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendVoice(
                targetChatId,
                it.voice().fileId()
            ).apply {
                it.caption() ?.let {
                    caption(it)
                }
                it.voice().also {
                    it.duration() ?.let {
                        duration(it)
                    }
                }
            }
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}
