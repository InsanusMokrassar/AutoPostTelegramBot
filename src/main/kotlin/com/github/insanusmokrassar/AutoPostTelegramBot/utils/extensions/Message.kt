package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.MessageEntity

@Deprecated("Now all this functionality is built-in Telegram Bot API")
private class MessageEntityDeformatter(
    private val sourceText: String,
    val range: IntRange,
    private val messageEntity: MessageEntity? = null
) {
    val deformatted: String by lazy {
        messageEntity ?.asMarkdownSource ?: sourceText.substring(
            range
        ).toMarkdown()
    }
}

@Deprecated(
    "Now all this functionality is built-in Telegram Bot API",
    ReplaceWith("asMarkdownSource")
)
private fun MessageEntity.asPreformattedPart(stringToModify: String): String = asMarkdownSource
