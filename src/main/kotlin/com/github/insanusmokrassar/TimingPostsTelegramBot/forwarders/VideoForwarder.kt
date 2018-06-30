package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendVideo

class VideoForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. video() != null && message.mediaGroupId == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        messages.mapNotNull {
            it.message
        }.map {
            SendVideo(
                targetChatId,
                it.video().fileId()
            ).apply {
                caption(it.caption())
                parseMode(ParseMode.Markdown)
            }
        }.forEach {
            bot.execute(it)
        }
    }
}