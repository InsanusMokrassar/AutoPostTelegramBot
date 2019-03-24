package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.media.SendMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.media.membersCountInMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.Message
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MediaGroupContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.PhotoContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.VideoContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.ref.WeakReference

typealias PostIdListPostMessagesTelegramMessages = Pair<Int, Map<PostMessage, Message>>
private typealias ChatIdMessageIdPair = Pair<ChatId, MessageIdentifier>

@Serializable
class PostPublisher : Publisher {
    @Transient
    val postPublishedChannel = BroadcastChannel<PostIdListPostMessagesTelegramMessages>(
        Channel.CONFLATED
    )

    @Transient
    private var botWR: WeakReference<RequestsExecutor>? = null

    @Transient
    private var sourceChatId: ChatId? = null
    @Transient
    private var targetChatId: ChatId? = null
    @Transient
    private var logsChatId: ChatId? = null

    @Transient
    private var publishPostCommand: PublishPost? = null

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(executor).also {
            publishPostCommand = PublishPost(
                pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser,
                pluginManager.plugins.firstOrNull { it is Publisher } as Publisher,
                it,
                baseConfig.logsChatId
            )
        }

        sourceChatId = baseConfig.sourceChatId
        targetChatId = baseConfig.targetChatId
        logsChatId = baseConfig.logsChatId
    }

    override suspend fun publishPost(postId: Int) {
        val executor = botWR ?.get() ?: return
        val sourceChatId: ChatId = sourceChatId ?: return
        val targetChatId: ChatId = targetChatId ?: return
        val logsChatId: ChatId = logsChatId ?: return

        val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

        try {
            executor.execute(
                SendMessage(
                    logsChatId,
                    "Start post"
                )
            ).asMessage.let {
                messagesToDelete.add(it.chat.id to it.messageId)
            }

            val messagesOfPost = mutableListOf<PostMessage>()

            PostsMessagesTable.getMessagesOfPost(postId).also {
                it.forEach { message ->
                    executor.executeUnsafe(
                        ForwardMessage(
                            sourceChatId,
                            logsChatId,
                            message.messageId,
                            disableNotification = true
                        ),
                        retries = 3
                    ) ?.asMessage ?.also {
                        messagesToDelete.add(it.chat.id to it.messageId)
                        message.message = it
                        messagesOfPost.add(message)
                    } ?: message.messageId.let {
                        commonLogger.warning(
                            "Can't forward message with id: $it; it will be removed from post"
                        )
                        PostsMessagesTable.removePostMessage(postId, it)
                    }
                }
            }
            if (messagesOfPost.isEmpty()) {
                PostsTable.removePost(postId)
                commonLogger.warning("Post $postId will be removed cause it contains not publishable messages")
                return
            }

            val responses = mutableListOf<Pair<PostMessage, Message>>()

            var mediaGroup: MutableList<PostMessage>? = null

            try {
                messagesOfPost.forEach { postMessage ->
                    //TODO:: REFACTOR
                    mediaGroup?.let { currentMediaGroup ->
                        if (postMessage.mediaGroupId != currentMediaGroup.first().mediaGroupId) {
                            mediaGroup = null
                            responses += sendMediaGroup(executor, targetChatId, currentMediaGroup)
                            null
                        } else {
                            currentMediaGroup.add(postMessage)
                        }
                    } ?: also {
                        if (postMessage.mediaGroupId != null) {
                            mediaGroup = mutableListOf<PostMessage>().apply {
                                add(postMessage)
                            }
                        } else {
                            (postMessage.message as? ContentMessage<*>)?.content?.createResends(
                                targetChatId
                            )?.forEach { request ->
                                responses.add(postMessage to executor.execute(request).asMessage)
                            }
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

            mediaGroup ?.also {
                responses += sendMediaGroup(executor, targetChatId, it)
            }

            responses.also {
                it.forEach { (_, message) ->
                    try {
                        executor.execute(
                            ForwardMessage(
                                message.chat.id,
                                logsChatId,
                                message.messageId
                            )
                        )
                    } catch (e: Exception) {
                        commonLogger.warning(
                            "Can't forward message with id: ${message.messageId}"
                        )
                    }
                }
                postPublishedChannel.send(
                    PostIdListPostMessagesTelegramMessages(
                        postId,
                        it.toMap()
                    )
                )
            }

            deletePost(
                executor,
                sourceChatId,
                postId
            )
        } catch (e: Throwable) {
            sendToLogger(e, "Publish post")
        } finally {
            messagesToDelete.forEach {
                executor.executeUnsafe(
                    DeleteMessage(
                        it.first,
                        it.second
                    )
                )
            }
        }
    }

    private suspend fun sendMediaGroup(
        executor: RequestsExecutor,
        targetChatId: ChatIdentifier,
        mediaGroup: List<PostMessage>
    ): List<Pair<PostMessage, Message>> {
        return when {
            mediaGroup.size < 2 -> {
                val postMessage = mediaGroup.firstOrNull() ?: return emptyList()
                val contentMessage = (postMessage.message as? ContentMessage<*>) ?: return emptyList()
                val request = contentMessage.content.createResend(
                    targetChatId
                )
                val response = executor.execute(request)
                listOf(
                    postMessage to response.asMessage
                )
            }
            mediaGroup.size in membersCountInMediaGroup -> {
                val mediaGroupContent = mediaGroup.mapNotNull {
                    ((it.message as? ContentMessage<*>) ?.content as? MediaGroupContent) ?.toMediaGroupMemberInputMedia() ?.let { media ->
                        it to media
                    }
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
}