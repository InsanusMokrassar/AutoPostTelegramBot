package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostIdMessageId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginName
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostsUsedTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageIdentifier
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private val PostsLikesMessagesTableScope = NewDefaultCoroutineScope()

class PostsLikesMessagesTable(
    private val postsLikesTable: PostsLikesTable
) : Table() {
    val ratingMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(Channel.CONFLATED)
    val ratingMessageUnregisteredChannel = BroadcastChannel<Int>(Channel.CONFLATED)

    private val postId = integer("postId").primaryKey()
    private val messageId = long("messageId")

    fun messageIdByPostId(postId: Int): MessageIdentifier? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.postId.eq(postId)
            }.firstOrNull() ?.get(messageId)
        }
    }

    fun postIdByMessageId(messageId: MessageIdentifier): Int? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.let {
                it[postId]
            }
        }
    }

    fun enableLikes(postId: Int, messageId: MessageIdentifier): Boolean {
        return transaction {
            if (messageIdByPostId(postId) == null) {
                insert {
                    it[this@PostsLikesMessagesTable.postId] = postId
                    it[this@PostsLikesMessagesTable.messageId] = messageId
                }
                PostsLikesMessagesTableScope.launch {
                    ratingMessageRegisteredChannel.send(
                        PostIdMessageId(
                            postId,
                            messageId
                        )
                    )
                }
                true
            } else {
                false
            }
        }
    }

    fun disableLikes(postId: Int) {
        transaction {
            (deleteWhere {
                this@PostsLikesMessagesTable.postId.eq(postId)
            } > 0).also {
                if (it) {
                    PostsLikesMessagesTableScope.launch {
                        ratingMessageUnregisteredChannel.send(postId)
                    }
                }
            }
        }
    }

    fun getEnabledPostsIdAndRatings(): List<RatingPair> {
        return transaction {
            selectAll().mapNotNull {
                it[postId]
            }.map {
                it.toLong() to postsLikesTable.getPostRating(it).toFloat()
            }
        }
    }
}
