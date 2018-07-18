package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendPhoto

class PhotoForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. photo() != null && message.mediaGroupId == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
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
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}