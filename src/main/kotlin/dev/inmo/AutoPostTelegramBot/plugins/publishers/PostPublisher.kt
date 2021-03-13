package dev.inmo.AutoPostTelegramBot.plugins.publishers

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.models.PostMessage
import dev.inmo.AutoPostTelegramBot.base.plugins.*
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.deletePost
import dev.inmo.AutoPostTelegramBot.utils.cacheMessagesToMap
import dev.inmo.AutoPostTelegramBot.utils.extensions.sendToLogger
import dev.inmo.AutoPostTelegramBot.utils.resend
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.DeleteMessage
import dev.inmo.tgbotapi.requests.ForwardMessage
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
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

    @Transient
    private lateinit var postsTable: PostsBaseInfoTable
    @Transient
    private lateinit var postsMessagesTable: PostsMessagesInfoTable

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(executor).also {
            publishPostCommand = PublishPost(
                pluginManager.findFirstPlugin(),
                pluginManager.findFirstPlugin() ?: throw IllegalStateException("Plugin `PostPublisher` can't be inited: there is no Publisher plugin"),
                it,
                baseConfig.logsChatId,
                baseConfig.postsTable
            )
        }

        sourceChatId = baseConfig.sourceChatId
        targetChatId = baseConfig.targetChatId
        logsChatId = baseConfig.logsChatId

        postsTable = baseConfig.postsTable
        postsMessagesTable = baseConfig.postsMessagesTable
    }

    override suspend fun publishPost(postId: Int) {
        val executor = botWR ?.get() ?: return
        val sourceChatId: ChatId = sourceChatId ?: return
        val targetChatId: ChatId = targetChatId ?: return
        val logsChatId: ChatId = logsChatId ?: return

        val messagesToDelete = mutableListOf<ChatIdMessageIdPair>()

        try {
            executor.execute(
                SendTextMessage(
                    logsChatId,
                    "Start post"
                )
            ).let {
                messagesToDelete.add(it.chat.id to it.messageId)
            }

            val messagesOfPost = postsMessagesTable.getMessagesOfPost(postId).asSequence().associate {
                it.messageId to it
            }.toMutableMap()

            val messages = cacheMessagesToMap(
                executor,
                sourceChatId,
                logsChatId,
                messagesOfPost.keys
            )

            messages.forEach { (id, message) ->
                messagesOfPost[id] ?.message = message
            }

            messagesOfPost.filter { (_, postMessage) ->
                val message = postMessage.message
                message == null || message !is ContentMessage<*>
            }.forEach { (messageId, _) ->
                commonLogger.warning(
                    "Can't forward message with id: $messageId; it will be removed from post"
                )
                postsMessagesTable.removePostMessage(postId, messageId)
                messagesOfPost.remove(messageId)
            }

            messagesOfPost.forEach { (messageId, _) ->
                messagesToDelete.add(sourceChatId to messageId)
            }

            val contentMessages = messages.asSequence().mapNotNull { it.value as? ContentMessage<*> }.associate { it.messageId to it }
            if (contentMessages.isEmpty()) {
                postsTable.removePost(postId)
                commonLogger.warning("Post $postId will be removed cause it contains not publishable messages")
                return
            }

            val mediaGroups = mutableMapOf<MediaGroupIdentifier, MutableList<PostMessage>>()
            messagesOfPost.forEach { (_, postMessage) ->
                postMessage.mediaGroupId ?.let { mediaGroupId ->
                    (mediaGroups[mediaGroupId] ?: mutableListOf<PostMessage>().also {
                        mediaGroups[mediaGroupId] = it
                    }).add(postMessage)
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
                postId,
                postsTable,
                postsMessagesTable
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