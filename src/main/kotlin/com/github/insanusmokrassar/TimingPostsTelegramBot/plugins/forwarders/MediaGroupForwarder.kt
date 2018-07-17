package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.SendMediaGroup

class MediaGroupForwarder : Forwarder {

    override val importance: Int = HIGH_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.mediaGroupId != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        val mediaGroups = mutableMapOf<String, MutableList<Message>>()
        messages.forEach {
            postMessage ->
            val message = postMessage.message ?: return@forEach
            val mediaGroupId = postMessage.mediaGroupId ?: return@forEach
            (mediaGroups[mediaGroupId] ?: mutableListOf<Message>().apply {
                mediaGroups[mediaGroupId] = this
            }).add(message)
        }

        return mediaGroups.values.map {
            SendMediaGroup(
                targetChatId,
                *it.mapNotNull {
                    (it.photo() ?.let {
                        it.maxBy { it.fileSize() } ?. fileId() ?.let {
                            InputMediaPhoto(
                                it
                            )
                        }
                    } ?: it.video() ?. let {
                        InputMediaVideo(
                            it.fileId()
                        )
                    }) ?.apply {
                        caption(it.caption())
                        parseMode(ParseMode.Markdown)
                    }
                }.toTypedArray()
            )
        }.flatMap {
            bot.execute(it).messages() ?.mapNotNull {
                it.messageId()
            } ?: emptyList()
        }
    }
}