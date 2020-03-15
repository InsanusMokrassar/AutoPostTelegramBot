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

@Deprecated("Deprecated due to replacement into config")
lateinit var PostsMessagesTable: PostsMessagesInfoTable
    internal set

class PostsMessagesInfoTable(private val database: Database) : Table() {
    private val coroutinesScope = NewDefaultCoroutineScope()
    private val newMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)

    private val removedMessagesOfPost = BroadcastChannel<PostIdToMessagesIds>(mediumBroadcastCapacity)
    private val removedMessageOfPost = BroadcastChannel<PostIdMessageId>(mediumBroadcastCapacity)

    val newMessagesOfPostFlow = newMessagesOfPost.asFlow()
    val removedMessagesOfPostFlow = removedMessagesOfPost.asFlow()
    val removedMessageOfPostFlow = removedMessageOfPost.asFlow()

    private val messageIdColumn = long("messageId").primaryKey()
    private val mediaGroupIdColumn = text("mediaGroupId").nullable()
    private val postIdColumn = integer("postId")

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(this@PostsMessagesInfoTable)
        }
    }

    fun getMessagesOfPost(postId: PostId): List<PostMessage> {
        return transaction(database) {
            select {
                postIdColumn.eq(postId)
            }.map {
                PostMessage(
                    it[messageIdColumn],
                    it[mediaGroupIdColumn]
                )
            }
        }
    }

    fun findPostByMessageId(messageId: MessageIdentifier): PostId? {
        return transaction(database) {
            select {
                messageIdColumn.eq(messageId)
            }.firstOrNull() ?.get(postIdColumn)
        }
    }

    fun addMessagesToPost(postId: PostId, vararg messages: PostMessage) {
        transaction(database) {
            messages.map {
                message ->
                insert {
                    it[postIdColumn] = postId
                    it[messageIdColumn] = message.messageId
                    it[mediaGroupIdColumn] = message.mediaGroupId
                }
                message.messageId
            }.let {
                coroutinesScope.launch {
                    newMessagesOfPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    fun removePostMessages(postId: PostId) {
        getMessagesOfPost(postId).let {
            transaction(database) {
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
        return transaction(database) {
            deleteWhere { messageIdColumn.eq(messageId) } > 0
        }.also {
            if (it) {
                notifyMessagesRemoved(postId, listOf(messageId))
            }
        }
    }

    private fun notifyMessagesRemoved(postId: PostId, messagesIds: List<MessageIdentifier>) {
        if (messagesIds.isNotEmpty()) {
            coroutinesScope.launch {
                removedMessagesOfPost.send(postId to messagesIds.toList())
            }
            coroutinesScope.launch {
                messagesIds.forEach { messageId ->
                    removedMessageOfPost.send(PostIdMessageId(postId, messageId))
                }
            }
        }
    }
}