package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.UnlimitedBroadcastChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.launch
import org.h2.jdbc.JdbcSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdRatingPair = Pair<Int, Int>
typealias PostIdUserId = Pair<Int, ChatId>

private const val resultColumnName = "result"

private val PostsLikesTableScope = NewDefaultCoroutineScope()

class PostsLikesTable : Table() {
    val likesChannel: BroadcastChannel<PostIdUserId> = UnlimitedBroadcastChannel()
    val dislikesChannel: BroadcastChannel<PostIdUserId> = UnlimitedBroadcastChannel()
    val ratingsChannel: BroadcastChannel<PostIdRatingPair> = UnlimitedBroadcastChannel()

    private val userId = long("userId").primaryKey()
    private val postId = integer("postId").primaryKey()
    private val like = bool("like").default(false)

    internal lateinit var postsLikesMessagesTable: PostsLikesMessagesTable

    init {
        PostsTable.postRemovedChannel.subscribe(
            {
                commonLogger.throwing(
                    "PostsLikesTable",
                    "Clear likes",
                    it
                )
                true
            }
        ) {
            clearPostMarks(it)
        }
    }

    fun userLikePost(userId: ChatId, postId: Int) {
        userLike(userId, postId, true)

        PostsLikesTableScope.launch {
            likesChannel.send(
                PostIdUserId(
                    postId,
                    userId
                )
            )
        }
    }

    fun userDislikePost(userId: ChatId, postId: Int) {
        userLike(userId, postId, false)

        PostsLikesTableScope.launch {
            dislikesChannel.send(
                PostIdUserId(
                    postId,
                    userId
                )
            )
        }
    }

    fun postLikes(postId: Int): Int = postLikeCount(postId, true)

    fun postDislikes(postId: Int): Int = postLikeCount(postId, false)

    fun getPostRating(postId: Int): Int {
        return transaction {
            try {
                exec("SELECT (likes-dislikes) as $resultColumnName FROM " +
                    "(SELECT count(*) as likes FROM ${nameInDatabaseCase()} WHERE ${this@PostsLikesTable.postId.name}=$postId AND \"${like.name}\"=${like.columnType.valueToString(true)}), " +
                    "(SELECT count(*) as dislikes FROM ${nameInDatabaseCase()} WHERE ${this@PostsLikesTable.postId.name}=$postId AND \"${like.name}\"=${like.columnType.valueToString(false)});") {
                    if (it.first()) {
                        it.getInt(it.findColumn(resultColumnName))
                    } else {
                        0
                    }
                } ?: 0
            } catch (e: JdbcSQLException) {
                select {
                    createChooser(postId, like = true)
                }.count() - select {
                    createChooser(postId, like = false)
                }.count()
            }
        }
    }

    fun getMostRated(): List<Int> {
        return transaction {
            postsLikesMessagesTable.getEnabledPostsIdAndRatings().let {
                var maxRating = Int.MIN_VALUE
                ArrayList<Int>().apply {
                    it.forEach {
                        val currentRating = it.second
                        if (currentRating > maxRating) {
                            maxRating = currentRating
                            clear()
                        }
                        if (currentRating == maxRating) {
                            add(it.first)
                        }
                    }
                }
            }
        }
    }

    /**
     * @param min Included. If null - always true
     * @param max Included. If null - always true
     *
     * @return Pairs with postId to Rate
     */
    fun getRateRange(min: Int?, max: Int?): List<PostIdRatingPair> {
        return postsLikesMessagesTable.getEnabledPostsIdAndRatings().sortedByDescending {
            (_, rating) ->
            rating
        }.filter {
            (_, rating) ->
            min ?.let { it <= rating } != false && max ?.let { rating <= it } != false
        }
    }

    private fun postLikeCount(postId: Int, like: Boolean): Int = transaction {
        select {
            this@PostsLikesTable.postId.eq(postId).and(this@PostsLikesTable.like.eq(like))
        }.count()
    }

    private fun createChooser(postId: Int, userId: ChatId? = null, like: Boolean? = null): Op<Boolean> {
        return this@PostsLikesTable.postId.eq(postId).let {
            userId ?.chatId ?.let {
                userId ->
                it.and(this@PostsLikesTable.userId.eq(userId))
            } ?: it
        }.let {
            like ?. let {
                like ->
                it.and(this@PostsLikesTable.like.eq(like))
            } ?: it
        }
    }

    private fun userLike(userId: ChatId, postId: Int, like: Boolean) {
        val chooser = createChooser(postId, userId)
        transaction {
            val record = select {
                chooser
            }.firstOrNull()
            record ?.let {
                if (it[this@PostsLikesTable.like] == like) {
                    deleteWhere { chooser }
                } else {
                    update(
                        {
                            chooser
                        }
                    ) {
                        it[this@PostsLikesTable.like] = like
                    }
                }
            } ?:let {
                addUser(userId, postId, like)
            }
            PostsLikesTableScope.launch {
                ratingsChannel.send(
                    PostIdRatingPair(postId, getPostRating(postId))
                )
            }
        }
    }

    private fun addUser(userId: ChatId, postId: Int, like: Boolean) {
        transaction {
            insert {
                it[this@PostsLikesTable.postId] = postId
                it[this@PostsLikesTable.userId] = userId.chatId
                it[this@PostsLikesTable.like] = like
            }
        }
    }

    internal fun clearPostMarks(postId: Int) {
        transaction {
            deleteWhere { this@PostsLikesTable.postId.eq(postId) }
        }
    }
}