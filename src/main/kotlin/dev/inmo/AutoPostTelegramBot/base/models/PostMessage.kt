package dev.inmo.AutoPostTelegramBot.base.models

import dev.inmo.tgbotapi.extensions.utils.asMediaGroupMessage
import dev.inmo.tgbotapi.types.MediaGroupIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.MediaGroupMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message

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

    constructor(message: Message) : this(message.messageId, message.asMediaGroupMessage() ?.mediaGroupId) {
        this.message = message
    }
}
