package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.SendMediaGroup
import java.io.IOException

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
            bot.execute(it).let {
                response ->
                response.messages() ?.mapNotNull {
                    it.messageId()
                } ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }
    }
}