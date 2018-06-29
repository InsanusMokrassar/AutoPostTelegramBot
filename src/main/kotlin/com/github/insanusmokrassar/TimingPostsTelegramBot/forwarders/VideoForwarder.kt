package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendVideo

class VideoForwarder : Forwarder {
    override fun canForward(message: Message): Boolean {
        return message.video() != null && message.mediaGroupId() == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: Message) {
        messages.map {
            SendVideo(
                targetChatId,
                it.video().fileId()
            )
        }.forEach {
            bot.execute(it)
        }
    }
}