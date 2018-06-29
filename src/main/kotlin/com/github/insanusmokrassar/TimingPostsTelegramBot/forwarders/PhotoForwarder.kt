package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.makePhotoLink
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendPhoto

class PhotoForwarder(
    private val botToken: String
) : Forwarder {
    override fun canForward(message: Message): Boolean {
        return message.photo() != null && message.mediaGroupId() == null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: Message) {
        messages.forEach {
            it.photo().maxBy { it.fileSize() } ?.let {
                bot.execute(
                    GetFile(
                        it.fileId()
                    )
                ).file().filePath().let {
                    bot.execute(
                        SendPhoto(
                            targetChatId,
                            makePhotoLink(
                                botToken,
                                it
                            )
                        )
                    )
                }
            }
        }
    }
}