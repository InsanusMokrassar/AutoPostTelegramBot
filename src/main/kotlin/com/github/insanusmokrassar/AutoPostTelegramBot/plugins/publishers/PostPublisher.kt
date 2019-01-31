package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.UnlimitedBroadcastChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.sendToLogger
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestException
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.ForwardMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.media.SendMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.types.*
import com.github.insanusmokrassar.TelegramBotAPI.types.files.biggest
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MediaGroupContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.PhotoContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.media.VideoContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
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
    val postPublishedChannel = UnlimitedBroadcastChannel<PostIdListPostMessagesTelegramMessages>()

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

            val messageToPost = PostsMessagesTable.getMessagesOfPost(postId).also {
                if (it.isEmpty()) {
                    PostsTable.removePost(postId)
                    return
                }
                it.forEach { message ->
                    try {
                        executor.execute(
                            ForwardMessage(
                                sourceChatId,
                                logsChatId,
                                message.messageId,
                                disableNotification = true
                            )
                        ).asMessage.also {
                            messagesToDelete.add(it.chat.id to it.messageId)
                            message.message = it
                        }
                    } catch (e: Exception) {
                        commonLogger.warning(
                            "Can't forward message with id: ${message.messageId}"
                        )
                    }
                }
            }

            val responses = mutableListOf<Pair<PostMessage, Message>>()

            var mediaGroup: MutableList<PostMessage>? = null

            try {
                messageToPost.forEach { postMessage ->
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
                logsChatId,
                postId
            )
        } catch (e: Throwable) {
            sendToLogger(e, "Publish post")
        } finally {
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

    private suspend fun sendMediaGroup(
        executor: RequestsExecutor,
        targetChatId: ChatIdentifier,
        mediaGroup: List<PostMessage>
    ): List<Pair<PostMessage, Message>> {
        val media = mediaGroup.mapNotNull {
            ((it.message as? ContentMessage<*>) ?.content as? MediaGroupContent) ?.toMediaGroupMemberInputMedia() ?.let { media ->
                it to media
            }
        }.toMap()
        return executor.execute(
            SendMediaGroup(
                targetChatId,
                media.values.toList()
            )
        ).mapNotNull {
            it.asMessage as? MediaGroupMessage
        }.mapNotNull {
            val content = it.content
            when (content) {
                is PhotoContent -> media.keys.firstOrNull { postMessage ->
                    media[postMessage] ?.file == content.media.biggest() ?.fileId
                }
                is VideoContent -> media.keys.firstOrNull { postMessage ->
                    media[postMessage] ?.file == content.media.fileId
                }
                else -> null
            } ?.let { postMessage ->
                postMessage to it
            }
        }
    }
}