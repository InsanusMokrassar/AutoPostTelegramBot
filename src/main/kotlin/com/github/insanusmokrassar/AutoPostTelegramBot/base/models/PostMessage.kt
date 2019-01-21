package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.TelegramBotAPI.types.MediaGroupIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.MediaGroupMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.Message

data class PostMessage(
    val messageId: MessageIdentifier,
    val mediaGroupId: MediaGroupIdentifier? = null
) {
    var message: Message? = null
        set(value) {
            field ?.let {
                throw IllegalStateException("Message already initialized")
            }
            field = value
        }

    constructor(message: Message) : this(message.messageId, (message as? MediaGroupMessage) ?.mediaGroupId) {
        this.message = message
    }
}
