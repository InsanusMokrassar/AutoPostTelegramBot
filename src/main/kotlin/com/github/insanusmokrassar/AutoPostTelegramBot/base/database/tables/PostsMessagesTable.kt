package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.extraSmallBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.largeBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdToMessagesIds = Pair<Int, Collection<MessageIdentifier>>

class PostsMessagesTable(
    tableName: String = "",
    postsTable: PostsTable
) : Table(tableName) {
    private val postsMessagesTableScope = NewDefaultCoroutineScope()

    val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(largeBroadcastCapacity)

    @Deprecated("This channel is not determine post id", ReplaceWith("removedMessageOfPost"))
    val removeMessageOfPost = BroadcastChannel<MessageIdentifier>(extraSmallBroadcastCapacity)
    val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(extraSmallBroadcastCapacity)
    val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(extraSmallBroadcastCapacity)

    private val messageId = long("messageId").primaryKey()
    private val mediaGroupId = text("mediaGroupId").nullable()
    private val postId = integer("postId")

    init {
        postsTable.postRemovedChannel.subscribe(
            scope = postsMessagesTableScope
        ) {
            removePostMessages(it)
        }
    }

    fun getMessagesOfPost(postId: Int): List<PostMessage> {
        return transaction {
            select {
                this@PostsMessagesTable.postId.eq(postId)
            }.map {
                PostMessage(
                    it[messageId].toLong(),
                    it[mediaGroupId]
                )
            }
        }
    }

    fun findPostByMessageId(messageId: MessageIdentifier): Int? {
        return transaction {
            select {
                this@PostsMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.get(postId)
        }
    }

    fun addMessagesToPost(postId: Int, vararg messages: PostMessage) {
        transaction {
            messages.map {
                message ->
                insert {
                    it[this@PostsMessagesTable.postId] = postId
                    it[messageId] = message.messageId
                    it[mediaGroupId] = message.mediaGroupId
                }
                message.messageId
            }.let {
                postsMessagesTableScope.launch {
                    newMessagesOfPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    @Deprecated("This method will be deprecated in near releases", ReplaceWith("removePostMessage"))
    fun removeMessageOfPost(messageId: MessageIdentifier) {
        transaction {
            if (deleteWhere { this@PostsMessagesTable.messageId.eq(messageId) } > 0) {
                postsMessagesTableScope.launch {
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
                postsMessagesTableScope.launch {
                    removedMessagesOfPost.send(postId to it)
                }
            }
        }
    }

    fun removePostMessage(postId: Int, messageId: MessageIdentifier): Boolean {
        return transaction {
            if (deleteWhere { this@PostsMessagesTable.messageId.eq(messageId) } > 0) {
                postsMessagesTableScope.launch {
                    removedMessageOfPost.send(postId to messageId)
                }
                true
            } else {
                false
            }
        }
    }
}