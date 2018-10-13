package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.MessageEntity

private class MessageEntityDeformatter(
    private val sourceText: String,
    val range: IntRange,
    private val messageEntity: MessageEntity? = null
) {
    val deformatted: String by lazy {
        messageEntity?.asPreformattedPart(
            sourceText
        ) ?: sourceText.substring(
            range
        ).toMarkdown()
    }
}

private fun MessageEntity.asPreformattedPart(stringToModify: String): String {
    val range = offset() until (offset() + length())
    val text = stringToModify.substring(range)
    return when(type()) {
        MessageEntity.Type.url -> "[$text](${url()})"
        MessageEntity.Type.bold -> "*$text*"
        MessageEntity.Type.italic -> "_${text}_"
        MessageEntity.Type.code -> "`$text`"
        MessageEntity.Type.pre -> "```$text```"
        MessageEntity.Type.text_link -> "[$text](${url()})"
        else -> text.toMarkdown()
    }
}

fun Message.textOrCaptionToMarkdown(): String {
    val text = caption() ?: text()
    val entities: List<MessageEntity> = (captionEntities() ?: entities()) ?.sortedBy { it.offset() } ?: kotlin.collections.emptyList()

    val deformatters = kotlin.collections.mutableListOf<MessageEntityDeformatter>()

    fun previousLast(): Int = deformatters.lastOrNull() ?.range ?.last ?.plus(1) ?: 0

    entities.forEach {
        val simpleTextRange = previousLast() until it.offset()
        if (!simpleTextRange.isEmpty()) {
            deformatters.add(
                MessageEntityDeformatter(
                    text,
                    simpleTextRange
                )
            )
        }
        deformatters.add(
            MessageEntityDeformatter(
                text,
                it.offset() until (it.offset() + it.length()),
                it
            )
        )
    }
    val simpleTextRange = previousLast() until text.length
    if (!simpleTextRange.isEmpty()) {
        deformatters.add(
            MessageEntityDeformatter(
                text,
                simpleTextRange
            )
        )
    }

    return deformatters.joinToString("", "", "") {
        it.deformatted
    }
}
