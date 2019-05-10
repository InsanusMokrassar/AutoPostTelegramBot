package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers.DislikeReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers.LikeReceiver
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

@Deprecated("Deprecated for the reason of extending by outside library")
@Serializable
class RatingPlugin : MutableRatingPlugin {
    @Transient
    private var likeReceiver: LikeReceiver? = null
    @Transient
    private var dislikeReceiver: DislikeReceiver? = null
    @Transient
    private var disableRating: DisableRating? = null
    @Transient
    private var enableRating: EnableRating? = null

    @Transient
    private var registeredRefresher: RegisteredRefresher? = null

    @Transient
    private var availableRates: AvailableRates? = null
    @Transient
    private var mostRated: MostRated? = null

    @Transient
    val postsLikesTable = PostsLikesTable()
    @Transient
    val postsLikesMessagesTable = PostsLikesMessagesTable(postsLikesTable).also {
        postsLikesTable.postsLikesMessagesTable = it
    }

    @Transient
    private lateinit var chatId: ChatId
    @Transient
    private lateinit var executor: WeakReference<RequestsExecutor>

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(postsLikesTable, postsLikesMessagesTable)
        }
    }

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        this.executor = WeakReference(executor)
        chatId = baseConfig.sourceChatId

        (pluginManager.plugins.firstOrNull { it is BasePlugin } as? BasePlugin)?.also {
            postsLikesMessagesTable.postsUsedTablePluginName = it.postsUsedTable to name
        }

        likeReceiver ?: let {
            likeReceiver = LikeReceiver(executor, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        dislikeReceiver ?: let {
            dislikeReceiver = DislikeReceiver(executor, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        disableRating ?: let {
            disableRating = DisableRating(executor, this)
        }
        enableRating ?: let {
            enableRating = EnableRating(executor, postsLikesMessagesTable, postsLikesTable)
        }

        mostRated ?:let {
            mostRated = MostRated(this.executor, postsLikesTable)
        }
        availableRates ?:let {
            availableRates = AvailableRates(this.executor, postsLikesMessagesTable)
        }

        registeredRefresher = RegisteredRefresher(
            baseConfig.sourceChatId,
            executor,
            this,
            postsLikesTable,
            postsLikesMessagesTable
        )
    }

    override suspend fun allocateRatingChangedFlow(): Flow<RatingPair> = postsLikesTable
        .ratingsChannel
        .asFlow()
        .map {
            it.first to it.second
        }

    override suspend fun allocateRatingRemovedFlow(): Flow<RatingPair> = postsLikesMessagesTable
        .ratingMessageUnregisteredChannel
        .asFlow()
        .map {
            it.toLong() to postsLikesTable.getPostRating(it)
        }

    override suspend fun allocateRatingAddedFlow(): Flow<PostIdRatingIdPair> = postsLikesMessagesTable
        .ratingMessageRegisteredChannel
        .asFlow()
        .map {
            it.first to it.first.toLong()
        }

    override suspend fun getRatingById(ratingId: RatingId): Int? = if (postsLikesMessagesTable.messageIdByPostId(ratingId.toInt()) != null) {
        postsLikesTable.getPostRating(ratingId.toInt())
    } else {
        null
    }

    override suspend fun resolvePostId(ratingId: RatingId): Int? = ratingId.toInt().let {
        if (postsLikesMessagesTable.messageIdByPostId(it) != null) {
            it
        } else {
            null
        }
    }

    override suspend fun getPostRatings(postId: PostId): List<RatingPair> = listOfNotNull(
        getRatingById(postId.toLong()) ?.let { postId.toLong() to it }
    )

    override suspend fun getRegisteredPosts(): List<PostId> = postsLikesMessagesTable.getEnabledPostsIdAndRatings().map {
        it.first.toInt()
    }

    override suspend fun deleteRating(ratingId: RatingId) {
        resolvePostId(ratingId) ?.let {
            postsLikesMessagesTable.disableLikes(it)
        }
    }

    override suspend fun addRatingFor(postId: PostId): RatingId? {
        if (postsLikesMessagesTable.messageIdByPostId(postId) == null) {
            refreshRegisteredMessage(
                chatId,
                executor.get() ?: return null,
                postId,
                postsLikesTable,
                postsLikesMessagesTable
            )
        }
        return getPostRatings(postId).firstOrNull() ?.first
    }
}
