package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendPhoto

class PhotoForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. photo() != null && message.mediaGroupId == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
            it.message
        }.mapNotNull {
            it.photo().maxBy { it.fileSize() } ?.let {
                SendPhoto(
                    targetChatId,
                    it.fileId()
                )
            } ?.apply {
                it.caption() ?.let {
                    caption(it)
                }
                parseMode(ParseMode.Markdown)
            }
        }.forEach {
            bot.execute(it)
        }
    }
}