package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import kotlinx.coroutines.flow.Flow

typealias RatingId = Long
typealias Rating = Float
typealias RatingPair = Pair<RatingId, Rating>
typealias PostIdRatingIdPair = Pair<PostId, RatingId>

interface RatingPlugin : Plugin {
    suspend fun allocateRatingChangedFlow(): Flow<RatingPair>
    suspend fun allocateRatingRemovedFlow(): Flow<RatingPair>
    suspend fun allocateRatingAddedFlow(): Flow<PostIdRatingIdPair>

    suspend fun getRatingById(ratingId: RatingId): Rating?
    suspend fun resolvePostId(ratingId: RatingId): PostId?

    suspend fun getPostRatings(postId: PostId): List<RatingPair>
    suspend fun getRegisteredPosts(): List<PostId>
}

interface MutableRatingPlugin : RatingPlugin {
    suspend fun deleteRating(ratingId: RatingId)
    suspend fun addRatingFor(postId: PostId): RatingId?
}

suspend fun RatingPlugin.getRatingRange(
    min: Int? = null,
    max: Int? = null
): Collection<RatingPair> = getRegisteredPosts().flatMap {
    getPostRatings(it)
}.let {
    if (min != null) {
        it.filter { (_, rating) ->
            rating >= min
        }
    } else {
        it
    }
}.let {
    if (max != null) {
        it.filter { (_, rating) ->
            rating <= max
        }
    } else {
        it
    }
}

suspend inline fun RatingPlugin.getRatingRange(
    range: IntRange
) = getRatingRange(
    range.first,
    range.last
)
