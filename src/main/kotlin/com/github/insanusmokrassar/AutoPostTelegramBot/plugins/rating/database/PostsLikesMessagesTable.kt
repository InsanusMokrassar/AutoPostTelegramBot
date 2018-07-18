package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostIdMessageId
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val subscriptionsCount = 256

class PostsLikesMessagesTable(
    private val postsLikesTable: PostsLikesTable
) : Table() {
    val ratingMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(subscriptionsCount)
    val ratingMessageUnregisteredChannel = BroadcastChannel<Int>(subscriptionsCount)

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
                launch {
                    ratingMessageRegisteredChannel.send(postId to messageId)
                }
                true
            } else {
                false
            }
        }
    }

    fun clearPostIdMessageId(postId: Int) {
        transaction {
            (deleteWhere {
                this@PostsLikesMessagesTable.postId.eq(postId)
            } > 0).also {
                if (it) {
                    launch {
                        ratingMessageUnregisteredChannel.send(postId)
                    }
                }
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
