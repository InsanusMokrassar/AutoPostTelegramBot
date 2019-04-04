package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.cacheMessages
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.resend
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

typealias PostIdListPostMessagesTelegramMessages = Pair<Int, List<Message>>
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

            val messagesOfPost = mutableListOf<PostMessage>().also {
                it.addAll(PostsMessagesTable.getMessagesOfPost(postId))
            }

            val messages = cacheMessages(
                executor,
                sourceChatId,
                logsChatId,
                PostsMessagesTable.getMessagesOfPost(postId).map { it.messageId }
            ).associate {
                it.messageId to it
            }

            messagesOfPost.associate {
                it.messageId to it
            }.also { associatedPostMessages ->
                messages.forEach { (id, message) ->
                    message.forwarded ?.messageId ?.let { realId ->
                        associatedPostMessages[realId] ?.message = message
                    } ?: id.let {
                        associatedPostMessages[id] ?.message = message
                    }
                }

                messagesOfPost.filter {
                    val message = it.message
                    message == null || message !is ContentMessage<*>
                }.forEach {
                    val messageId = it.messageId
                    commonLogger.warning(
                        "Can't forward message with id: $messageId; it will be removed from post"
                    )
                    PostsMessagesTable.removePostMessage(postId, messageId)
                    messagesOfPost.remove(it)
                }
            }
            messagesOfPost.forEach {
                messagesToDelete.add(sourceChatId to it.messageId)
            }

            val contentMessages = messages.asSequence().mapNotNull { it.value as? ContentMessage<*> }.associate { it.messageId to it }
            if (contentMessages.isEmpty()) {
                PostsTable.removePost(postId)
                commonLogger.warning("Post $postId will be removed cause it contains not publishable messages")
                return
            }

            val mediaGroups = mutableMapOf<MediaGroupIdentifier, MutableList<PostMessage>>()
            messagesOfPost.forEach {
                it.mediaGroupId ?.let { mediaGroupId ->
                    (mediaGroups[mediaGroupId] ?: mutableListOf<PostMessage>().also {
                        mediaGroups[mediaGroupId] = it
                    }).add(it)
                }
            }

            val responses = resend(
                executor,
                targetChatId,
                contentMessages.values,
                mediaGroups.values.map { it.mapNotNull { it.message ?.messageId } }
            )


            responses.forEach { (_, message) ->
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
                    responses.map { it.second }
                )
            )

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
}