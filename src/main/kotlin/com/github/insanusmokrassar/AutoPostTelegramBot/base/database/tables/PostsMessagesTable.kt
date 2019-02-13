package com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.extraSmallBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.largeBroadcastCapacity
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.chooseCapacity
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdToMessagesIds = Pair<Int, Collection<MessageIdentifier>>

val PostsMessagesTableScope = NewDefaultCoroutineScope()

object PostsMessagesTable : Table() {
    val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(chooseCapacity(largeBroadcastCapacity))

    @Deprecated("This channel is not determine post id", ReplaceWith("removedMessageOfPost"))
    val removeMessageOfPost = BroadcastChannel<MessageIdentifier>(chooseCapacity(extraSmallBroadcastCapacity))
    val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(chooseCapacity(extraSmallBroadcastCapacity))
    val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(chooseCapacity(extraSmallBroadcastCapacity))

    private val messageId = long("messageId").primaryKey()
    private val mediaGroupId = text("mediaGroupId").nullable()
    private val postId = integer("postId")

    fun getMessagesOfPost(postId: Int): List<PostMessage> {
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
            if (deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0) {
                PostsMessagesTableScope.launch {
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
                PostsMessagesTableScope.launch {
                    removedMessagesOfPost.send(postId to it)
                }
            }
        }
    }

    fun removePostMessage(postId: Int, messageId: MessageIdentifier): Boolean {
        return transaction {
            if (deleteWhere { PostsMessagesTable.messageId.eq(messageId) } > 0) {
                PostsMessagesTableScope.launch {
                    removedMessageOfPost.send(postId to messageId)
                }
                true
            } else {
                false
            }
        }
    }
}