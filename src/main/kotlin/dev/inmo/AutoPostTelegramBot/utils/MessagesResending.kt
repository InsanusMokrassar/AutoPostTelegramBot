package dev.inmo.AutoPostTelegramBot.utils

import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeAsync
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.send.media.SendMediaGroup
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.abstracts.*
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaGroupContent

private typealias ChatIdMessageIdPair = Pair<ChatId, MessageIdentifier>

suspend fun cacheMessagesToMap(
    executor: RequestsExecutor,
    sourceChatId: ChatId,
    cacheChatId: ChatId,
    messagesIds: Iterable<MessageIdentifier>,
    clear: Boolean = true
): Map<MessageIdentifier, PossiblyForwardedMessage> {
    val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

    return messagesIds.mapNotNull { id ->
        executor.executeUnsafe(
            ForwardMessage(
                sourceChatId,
                cacheChatId,
                id,
                disableNotification = true
            ),
            retries = 3
        ) ?.let {
            messagesToDelete.add(it.chat.id to it.messageId)
            id to it
        }
    }.toMap().also {
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
    messages: Iterable<ContentMessage<*>>,
    mediaGroups: List<List<MessageIdentifier>>
): MutableList<Pair<ContentMessage<*>, Message>> {
    val responses = mutableListOf<Pair<ContentMessage<*>, Message>>()

    val sendingMap = mutableMapOf<Request<*>, List<ContentMessage<*>>>()

    val leftMessages = messages.toMutableList()
    val leftMediaGroups = mediaGroups.toMutableList()

    while (leftMessages.isNotEmpty()) {
        val message = leftMessages.first()
        leftMediaGroups.firstOrNull {
            it.contains(message.messageId)
        } ?.let { mediaGroup ->

            val contents = leftMessages.filter {
                it.messageId in mediaGroup
            }.also {
                leftMessages.removeAll(it)
            }.mapNotNull { contentMessage ->
                (contentMessage.content as? MediaGroupContent) ?.let {
                    contentMessage to it
                }
            }.toMap()

            val requests = sendMediaGroup(
                executor,
                targetChatId,
                contents.values
            ).associate {
                it.first to it.second.map {
                    contents.keys.elementAt(contents.values.indexOf(it))
                }
            }

            sendingMap.putAll(requests)

            leftMediaGroups.remove(mediaGroup)
            leftMessages.removeAll { it in contents.keys }
        } ?: message.let {
            sendingMap[it.content.createResend(targetChatId)] = listOf(it)
            leftMessages.remove(message)
        }
    }

    try {
        sendingMap.forEach { (request, sourceMessages) ->
            responses += executor.execute(request).let {
                when (it) {
                    // media group
                    is List<*> -> it.mapNotNull {
                        val asMediaGroupMessage = (it as MediaGroupMessage<*>)
                        val fileUniqueId = asMediaGroupMessage.content.media.fileUniqueId
                        sourceMessages.firstOrNull { sourceMessage ->
                            (sourceMessage.content as? MediaGroupContent) ?.media ?.fileUniqueId == fileUniqueId
                        } ?.to(asMediaGroupMessage)
                    }
                    // common message
                    is Message -> sourceMessages.map { source ->
                        source to it
                    }
                    // something other
                    else -> emptyList()
                }
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
    return responses
}

private suspend fun sendMediaGroup(
    executor: RequestsExecutor,
    targetChatId: ChatIdentifier,
    contents: Collection<MediaGroupContent>
): List<Pair<Request<*>, List<MediaGroupContent>>> {
    return when {
        contents.size < 2 -> {
            val content = contents.firstOrNull() ?: return emptyList()
            listOf(
                content.createResend(
                    targetChatId
                ) to listOf(content)
            )
        }
        contents.size in mediaCountInMediaGroup -> {
            val mediaGroupContent = contents.map {
                it to it.toMediaGroupMemberInputMedia()
            }.toMap()
            listOf(
                SendMediaGroup<MediaGroupContent>(
                    targetChatId,
                    mediaGroupContent.values.toList()
                ) to contents.toList()
            )
        }
        else -> contents.chunked(mediaCountInMediaGroup.last).flatMap { postMessages ->
            sendMediaGroup(executor, targetChatId, postMessages)
        }
    }
}
