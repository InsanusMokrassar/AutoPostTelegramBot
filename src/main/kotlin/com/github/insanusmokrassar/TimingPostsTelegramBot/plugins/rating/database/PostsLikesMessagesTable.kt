package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class PostsLikesMessagesTable(
    private val postsLikesTable: PostsLikesTable
) : Table() {
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

    fun getEnabledPostsIdAndRatings(): List<PostIdRatingPair> {
        return transaction {
            selectAll().mapNotNull {
                it[postId]
            }.map {
                it to postsLikesTable.getPostRating(it)
            }
        }
    }
}
