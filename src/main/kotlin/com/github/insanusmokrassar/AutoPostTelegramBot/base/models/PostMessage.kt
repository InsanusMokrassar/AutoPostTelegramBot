package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.pengrad.telegrambot.model.Message

data class PostMessage(
    val messageId:Int,
    val mediaGroupId: String? = null
) {
    var message: Message? = null
        set(value) {
            field ?.let {
                throw IllegalStateException("Message already initialized")
            }
            field = value
        }

    constructor(message: Message) : this(message.messageId(), message.mediaGroupId()) {
        this.message = message
    }
}
