package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendPhoto

class PhotoForwarder : Forwarder {
    override fun canForward(message: Message): Boolean {
        return message.photo() != null && message.mediaGroupId() == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: Message) {
        messages.mapNotNull {
            it.photo().maxBy { it.fileSize() } ?.let {
                SendPhoto(
                    targetChatId,
                    it.fileId()
                )
            }
        }.forEach {
            bot.execute(it)
        }
    }
}