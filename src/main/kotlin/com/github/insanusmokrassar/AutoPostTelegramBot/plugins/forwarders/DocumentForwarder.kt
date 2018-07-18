package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendDocument

class DocumentForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. document() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendDocument(
                targetChatId,
                it.document().fileId()
            ).apply {
                it.caption() ?.let {
                    caption(it)
                }
                it.document().fileName() ?.let {
                    fileName(it)
                }
            }.parseMode(
                ParseMode.Markdown
            )
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}