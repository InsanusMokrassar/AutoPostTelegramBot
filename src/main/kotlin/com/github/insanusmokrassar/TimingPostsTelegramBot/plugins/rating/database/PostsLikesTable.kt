package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.pluginLogger
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.h2.jdbc.JdbcSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

typealias PostIdRatingPair = Pair<Int, Int>
typealias PostIdUserId = Pair<Int, Long>

private const val countOfSubscriptions = 256

private const val resultColumnName = "result"

object PostsLikesTable : Table() {
    val likesChannel = BroadcastChannel<PostIdUserId>(countOfSubscriptions)
    val dislikesChannel = BroadcastChannel<PostIdUserId>(countOfSubscriptions)
    val ratingsChannel = BroadcastChannel<PostIdRatingPair>(countOfSubscriptions)

    private val userId = long("userId").primaryKey()
    private val postId = integer("postId").references(PostsTable.id).primaryKey()
    private val like = bool("like").default(false)

    init {
        PostsTable.postRemovedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val removedPostId = it.receive()
                    try {
                        clearPostMarks(removedPostId)
                    } catch (e: Exception) {
                        pluginLogger.throwing(
                            "PostsLikesTable",
                            "Clear likes",
                            e
                        )
                    }
                }
                it.cancel()
            }
        }
    }

    fun userLikePost(userId: Long, postId: Int) {
        userLike(userId, postId, true)

        launch {
            likesChannel.send(postId to userId)
        }
    }

    fun userDislikePost(userId: Long, postId: Int) {
        userLike(userId, postId, false)

        launch {
            dislikesChannel.send(postId to userId)
        }
    }

    fun postLikes(postId: Int): Int = postLikeCount(postId, true)

    fun postDislikes(postId: Int): Int = postLikeCount(postId, false)

    fun getPostRating(postId: Int): Int {
        return transaction {
            try {
                exec("SELECT (likes-dislikes) as $resultColumnName FROM " +
                    "(SELECT count(*) as likes FROM ${nameInDatabaseCase()} WHERE ${PostsLikesTable.postId.name}=$postId AND \"${like.name}\"=${like.columnType.valueToString(true)}), " +
                    "(SELECT count(*) as dislikes FROM ${nameInDatabaseCase()} WHERE ${PostsLikesTable.postId.name}=$postId AND \"${like.name}\"=${like.columnType.valueToString(false)});") {
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
            PostsLikesMessagesTable.getEnabledPostsIdAndRatings().let {
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
        return PostsLikesMessagesTable.getEnabledPostsIdAndRatings().sortedByDescending {
            it.second
        }.filter {
            pair ->
            min ?.let { it <= pair.second } != false && max ?.let { pair.second <= it } != false
        }
    }

    private fun postLikeCount(postId: Int, like: Boolean): Int = transaction {
        select {
            PostsLikesTable.postId.eq(postId).and(PostsLikesTable.like.eq(like))
        }.count()
    }

    private fun createChooser(postId: Int, userId: Long? = null, like: Boolean? = null): Op<Boolean> {
        return PostsLikesTable.postId.eq(postId).let {
            userId ?.let {
                userId ->
                it.and(PostsLikesTable.userId.eq(userId))
            } ?: it
        }.let {
            like ?. let {
                like ->
                it.and(PostsLikesTable.like.eq(like))
            } ?: it
        }
    }

    private fun userLike(userId: Long, postId: Int, like: Boolean) {
        val chooser = createChooser(postId, userId)
        transaction {
            val record = select {
                chooser
            }.firstOrNull()
            record ?.let {
                if (it[PostsLikesTable.like] == like) {
                    deleteWhere { chooser }
                } else {
                    update(
                        {
                            chooser
                        }
                    ) {
                        it[PostsLikesTable.like] = like
                    }
                }
            } ?:let {
                addUser(userId, postId, like)
            }
            launch {
                ratingsChannel.send(
                    PostIdRatingPair(postId, getPostRating(postId))
                )
            }
        }
    }

    private fun addUser(userId: Long, postId: Int, like: Boolean) {
        transaction {
            insert {
                it[PostsLikesTable.postId] = postId
                it[PostsLikesTable.userId] = userId
                it[PostsLikesTable.like] = like
            }
        }
    }

    internal fun clearPostMarks(postId: Int) {
        transaction {
            deleteWhere { PostsLikesTable.postId.eq(postId) }
        }
    }
}