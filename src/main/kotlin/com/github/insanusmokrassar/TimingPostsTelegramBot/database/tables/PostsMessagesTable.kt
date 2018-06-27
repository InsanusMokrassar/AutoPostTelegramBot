package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NoRowFoundException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object PostsMessagesTable : Table() {
    private val messageId = integer("messageId").primaryKey()
    private val postId = integer("postId").references(PostsTable.id)

    @Throws(NoRowFoundException::class)
    fun getMessagesOfPost(postId: Int): List<Int> {
        return transaction {
            select {
                PostsMessagesTable.postId.eq(postId)
            }.map {
                it[messageId]
            }.also {
                if (it.isEmpty()) {
                    throw NoRowFoundException("No rows for $postId")
                }
            }
        }
    }

    fun addMessagesToPost(postId: Int, vararg messageIds: Int) {
        transaction {
            messageIds.forEach {
                messageId ->
                insert {
                    it[PostsMessagesTable.postId] = postId
                    it[PostsMessagesTable.messageId] = messageId
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