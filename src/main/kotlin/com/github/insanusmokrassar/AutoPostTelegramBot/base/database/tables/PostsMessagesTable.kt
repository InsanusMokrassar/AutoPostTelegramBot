package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.mediumBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdToMessagesIds = Pair<PostId, Collection<MessageIdentifier>>

private val PostsMessagesTableScope = NewDefaultCoroutineScope()

object PostsMessagesTable : Table() {
    private val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)

    private val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)
    private val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(mediumBroadcastCapacity)

    val newMessagesOfPostFlow = newMessagesOfPost.asFlow()
    val removedMessagesOfPostFlow = removedMessagesOfPost.asFlow()
    val removedMessageOfPostFlow = removedMessageOfPost.asFlow()

    private val messageId = long("messageId").primaryKey()
    private val mediaGroupId = text("mediaGroupId").nullable()
    private val postId = integer("postId")

    fun getMessagesOfPost(postId: PostId): List<PostMessage> {
        return transaction {
            select {
                PostsMessagesTable.postId.eq(postId)
            }.map {
                PostMessage(
                    it[messageId].toLong(),
                    it[mediaGroupId]
                )
            }
        }
    }

    fun findPostByMessageId(messageId: MessageIdentifier): PostId? {
        return transaction {
            select {
                this@PostsMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.get(postId)
        }
    }

    fun addMessagesToPost(postId: PostId, vararg messages: PostMessage) {
        transaction {
            messages.map {
                message ->
                insert {
                    it[PostsMessagesTable.postId] = postId
                    it[messageId] = message.messageId
                    it[mediaGroupId] = message.mediaGroupId
                }
                message.messageId
            }.let {
                PostsMessagesTableScope.launch {
                    newMessagesOfPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    @Deprecated("This method will be deprecated in near releases", ReplaceWith("removePostMessage"))
    fun removeMessageOfPost(messageId: MessageIdentifier) {
        transaction {
            select {
                this@PostsMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.get(postId) ?.also { _ ->
                deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0
            } ?: return@transaction null
        } ?.also { postId ->
            notifyMessagesRemoved(postId, listOf(messageId))
        }
    }

    fun removePostMessages(postId: PostId) {
        getMessagesOfPost(postId).let {
            transaction {
                it.mapNotNull {
                    postMessage ->
                    if (removePostMessage(postId, postMessage.messageId)) {
                        postMessage.messageId
                    } else {
                        null
                    }
                }
            }
        }.also {
            notifyMessagesRemoved(postId, it)
        }
    }

    fun removePostMessage(postId: PostId, messageId: MessageIdentifier): Boolean {
        return transaction {
            deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0
        }.also {
            if (it) {
                notifyMessagesRemoved(postId, listOf(messageId))
            }
        }
    }

    private fun notifyMessagesRemoved(postId: PostId, messagesIds: List<MessageIdentifier>) {
        if (messagesIds.isNotEmpty()) {
            PostsMessagesTableScope.launch {
                removedMessagesOfPost.send(postId to messagesIds.toList())
            }
            PostsMessagesTableScope.launch {
                messagesIds.forEach { messageId ->
                    removedMessageOfPost.send(PostIdMessageId(postId, messageId))
                }
            }
        }
    }
}