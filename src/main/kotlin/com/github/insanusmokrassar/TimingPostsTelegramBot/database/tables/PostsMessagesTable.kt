package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val countOfSubscriptions = 256

typealias PostIdToMessagesIds = Pair<Int, Collection<Int>>

object PostsMessagesTable : Table() {
    val addedMessagesToPost = BroadcastChannel<PostIdToMessagesIds>(countOfSubscriptions)

    private val messageId = integer("messageId").primaryKey()
    private val mediaGroupId = text("mediaGroupId").nullable()
    private val postId = integer("postId").references(PostsTable.id)

    @Throws(NoRowFoundException::class)
    fun getMessagesOfPost(postId: Int): List<PostMessage> {
        return transaction {
            select {
                PostsMessagesTable.postId.eq(postId)
            }.map {
                PostMessage(
                    it[messageId],
                    it[mediaGroupId]
                )
            }.also {
                if (it.isEmpty()) {
                    throw NoRowFoundException("No rows for $postId")
                }
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
                    addedMessagesToPost.send(PostIdToMessagesIds(postId, it))
                }
            }
        }
    }

    fun removeMessageOfPost(messageId: Int) {
        transaction {
            deleteWhere { PostsMessagesTable.messageId.eq(messageId) }
        }
    }
}