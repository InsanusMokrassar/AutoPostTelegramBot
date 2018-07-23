package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val countOfSubscriptions = 256

typealias PostIdToMessagesIds = Pair<Int, Collection<Int>>

object PostsMessagesTable : Table() {
    val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(countOfSubscriptions)

    @Deprecated("This channel is not determine post id", ReplaceWith("removedMessageOfPost"))
    val removeMessageOfPost = BroadcastChannel<Int>(countOfSubscriptions)
    val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(countOfSubscriptions)
    val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(countOfSubscriptions)

    private val messageId = integer("messageId").primaryKey()
    private val mediaGroupId = text("mediaGroupId").nullable()
    private val postId = integer("postId")

    fun getMessagesOfPost(postId: Int): List<PostMessage> {
        return transaction {
            select {
                PostsMessagesTable.postId.eq(postId)
            }.map {
                PostMessage(
                    it[messageId],
                    it[mediaGroupId]
                )
            }
        }
    }

    fun addMessagesToPost(postId: Int, vararg messages: PostMessage) {
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
                launch {
                    newMessagesOfPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    @Deprecated("This method will be deprecated in near releases", ReplaceWith("removePostMessage"))
    fun removeMessageOfPost(messageId: Int) {
        transaction {
            if (deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0) {
                launch {
                    removeMessageOfPost.send(messageId)
                }
            }
        }
    }

    fun removePostMessages(postId: Int) {
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
            if (it.isNotEmpty()) {
                launch {
                    removedMessagesOfPost.send(postId to it)
                }
            }
        }
    }

    fun removePostMessage(postId: Int, messageId: Int): Boolean {
        return transaction {
            if (deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0) {
                launch {
                    removedMessageOfPost.send(postId to messageId)
                }
                true
            } else {
                false
            }
        }
    }
}