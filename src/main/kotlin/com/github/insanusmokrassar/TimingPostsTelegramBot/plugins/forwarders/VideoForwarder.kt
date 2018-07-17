package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendVideo

class VideoForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. video() != null && message.mediaGroupId == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendVideo(
                targetChatId,
                it.video().fileId()
            ).apply {
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