package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object PostsLikesMessagesTable : Table() {
    private val postId = integer("postId").primaryKey()
    private val messageId = integer("messageId")

    fun messageIdByPostId(postId: Int): Int? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.postId.eq(postId)
            }.firstOrNull() ?.get(messageId)
        }
    }

    fun postIdByMessage(messageId: Int): Int? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.let {
                it[postId]
            }
        }
    }

    fun registerLikeMessageId(postId: Int, messageId: Int): Boolean {
        return transaction {
            if (messageIdByPostId(postId) == null) {
                insert {
                    it[this@PostsLikesMessagesTable.postId] = postId
                    it[this@PostsLikesMessagesTable.messageId] = messageId
                }
                true
            } else {
                false
            }
        }
    }

    fun clearPostIdMessageId(postId: Int) {
        transaction {
            deleteWhere {
                this@PostsLikesMessagesTable.postId.eq(postId)
            }
        }
    }
}
