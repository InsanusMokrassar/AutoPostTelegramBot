package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.media.SendMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.media.membersCountInMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.PhotoContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.VideoContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe

private typealias ChatIdMessageIdPair = Pair<ChatId, MessageIdentifier>

suspend fun cacheMessages(
    executor: RequestsExecutor,
    sourceChatId: ChatId,
    cacheChatId: ChatId,
    messagesIds: Iterable<MessageIdentifier>,
    clear: Boolean = true
): List<AbleToBeForwardedMessage> {
    val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

    return messagesIds.mapNotNull { message ->
        executor.executeUnsafe(
            ForwardMessage(
                sourceChatId,
                cacheChatId,
                message,
                disableNotification = true
            ),
            retries = 3
        ) ?.asMessage ?.also {
            messagesToDelete.add(it.chat.id to it.messageId)
        } as? AbleToBeForwardedMessage
    }.also {
        if (clear) {
            messagesToDelete.forEach {
                executor.executeAsync(
                    DeleteMessage(
                        it.first,
                        it.second
                    )
                )
            }
        }
    }
}

suspend fun resend(
    executor: RequestsExecutor,
    targetChatId: ChatId,
    messages: Iterable<ContentMessage<*>>
): MutableList<Pair<Message, Message>> {
    var mediaGroup: MutableList<MediaGroupMessage> = mutableListOf()
    val responses = mutableListOf<Pair<Message, Message>>()

    try {
        messages.forEach { message ->
            (message as? MediaGroupMessage) ?.let { mediaGroupMessage ->
                if (mediaGroup.firstOrNull() ?.mediaGroupId == mediaGroupMessage.mediaGroupId) {
                    mediaGroup.add(mediaGroupMessage)
                } else {
                    if (mediaGroup.isNotEmpty()) {
                        responses += sendMediaGroup(executor, targetChatId, mediaGroup)
                        mediaGroup = mutableListOf()
                    }
                    mediaGroup.add(mediaGroupMessage)
                }
            } ?: (message as? ContentMessage) ?.let { contentMessage ->
                if (mediaGroup.isNotEmpty()) {
                    responses += sendMediaGroup(executor, targetChatId, mediaGroup)
                    mediaGroup = mutableListOf()
                }
                responses.add(
                    contentMessage to executor.execute(
                        contentMessage.content.createResend(targetChatId)
                    ).asMessage
                )
            }
        }
    } catch (e: Exception) {
        responses.forEach { (_, response) ->
            executor.executeUnsafe(
                DeleteMessage(
                    response.chat.id,
                    response.messageId
                )
            )
        }
        throw e
    }

    if (mediaGroup.isNotEmpty()) {
        responses += sendMediaGroup(executor, targetChatId, mediaGroup)
        mediaGroup = mutableListOf()
    }
    return responses
}

private suspend fun sendMediaGroup(
    executor: RequestsExecutor,
    targetChatId: ChatIdentifier,
    mediaGroup: List<MediaGroupMessage>
): List<Pair<Message, Message>> {
    return when {
        mediaGroup.size < 2 -> {
            val message = mediaGroup.firstOrNull() ?: return emptyList()
            val request = message.content.createResend(
                targetChatId
            )
            val response = executor.execute(request)
            listOf(
                message to response.asMessage
            )
        }
        mediaGroup.size in membersCountInMediaGroup -> {
            val mediaGroupContent = mediaGroup.map {
                it to it.content.toMediaGroupMemberInputMedia()
            }.toMap()
            val request = SendMediaGroup(
                targetChatId,
                mediaGroupContent.values.toList()
            )
            val response = executor.execute(request)
            val contentResponse = response.mapNotNull { it.asMessage as? ContentMessage<*> }

            contentResponse.mapNotNull {
                val content = it.content
                when (content) {
                    is PhotoContent -> mediaGroupContent.keys.firstOrNull { postMessage ->
                        mediaGroupContent[postMessage] ?.file == content.media.fileId
                    }
                    is VideoContent -> mediaGroupContent.keys.firstOrNull { postMessage ->
                        mediaGroupContent[postMessage] ?.file == content.media.fileId
                    }
                    else -> null
                } ?.let { postMessage ->
                    postMessage to it
                }
            }
        }
        else -> mediaGroup.chunked(membersCountInMediaGroup.endInclusive).flatMap { postMessages ->
            sendMediaGroup(executor, targetChatId, postMessages)
        }
    }
}
