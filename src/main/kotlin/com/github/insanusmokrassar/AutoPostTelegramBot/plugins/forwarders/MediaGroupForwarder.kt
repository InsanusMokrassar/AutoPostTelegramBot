package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeSync
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

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        val mediaGroups = mutableMapOf<String, MutableList<PostMessage>>()
        messages.forEach {
            postMessage ->
            postMessage.message ?: return@forEach
            val mediaGroupId = postMessage.mediaGroupId ?: return@forEach
            (mediaGroups[mediaGroupId] ?: mutableListOf<PostMessage>().apply {
                mediaGroups[mediaGroupId] = this
            }).add(postMessage)
        }

        return mediaGroups.values.map {
            SendMediaGroup(
                targetChatId,
                *it.mapNotNull {
                    postMessage ->
                    postMessage.message ?.let {
                        message ->
                        (message.photo() ?.let {
                            it.maxBy { it.fileSize() } ?. fileId() ?.let {
                                InputMediaPhoto(
                                    it
                                )
                            }
                        } ?: message.video() ?. let {
                            InputMediaVideo(
                                it.fileId()
                            )
                        }) ?.apply {
                            caption(message.caption())
                            parseMode(ParseMode.Markdown)
                        }
                    }
                }.toTypedArray()
            ) to it
        }.flatMap {
            (request, originals) ->
            bot.executeSync(request).let {
                response ->
                response.messages() ?.let {
                    (0 until originals.size).map {
                        i ->
                        originals[i] to it[i]
                    }
                } ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }.toMap()
    }
}
