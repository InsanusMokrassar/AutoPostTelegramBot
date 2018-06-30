package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.SendMediaGroup

class MediaGroupForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.mediaGroupId != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage) {
        val mediaGroups = mutableMapOf<String, MutableList<Message>>()
        messages.forEach {
            postMessage ->
            val message = postMessage.message ?: return@forEach
            val mediaGroupId = postMessage.mediaGroupId ?: return@forEach
            (mediaGroups[mediaGroupId] ?: mutableListOf<Message>().apply {
                mediaGroups[mediaGroupId] = this
            }).add(message)
        }

        mediaGroups.values.map {
            SendMediaGroup(
                targetChatId,
                *it.mapNotNull {
                    it.photo() ?.let {
                        it.maxBy { it.fileSize() } ?. fileId() ?.let {
                            InputMediaPhoto(
                                it
                            )
                        }
                    } ?: it.video() ?. let {
                        InputMediaVideo(
                            it.fileId()
                        )
                    }
                }.toTypedArray()
            )
        }.forEach {
            bot.execute(it)
        }
    }
}