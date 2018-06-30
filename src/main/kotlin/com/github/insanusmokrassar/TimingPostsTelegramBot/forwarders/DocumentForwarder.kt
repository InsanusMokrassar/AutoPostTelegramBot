package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendDocument

class DocumentForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. document() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
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
        }.forEach {
            bot.execute(it)
        }
    }
}