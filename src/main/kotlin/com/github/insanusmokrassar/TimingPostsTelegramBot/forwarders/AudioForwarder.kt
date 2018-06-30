package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendAudio

class AudioForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. audio() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
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
        }.forEach {
            bot.execute(it)
        }
    }
}