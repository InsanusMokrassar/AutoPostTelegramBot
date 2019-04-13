package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CalculatedDateTime
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.lang.ref.WeakReference

private val zeroDateTime: DateTime by lazy {
    DateTime(0, DateTimeZone.UTC)
}

@Serializable
class GarbageCollector(
    val minimalRate: Int = -3,
    private val skipTime: String? = null,
    private val manualCheckTime: String? = null
) : Plugin {
    @Transient
    val skipDateTime: List<Pair<Long, Long>> by lazy {
        skipTime ?.parseDateTimes() ?.let {
                parsed ->
            if (parsed.size > 1) {
                parsed.asPairs()
            } else {
                parsed.firstOrNull() ?.let {
                        firstParsed ->
                    listOf(
                        CalculatedDateTime(
                            "",
                            zeroDateTime,
                            0L,
                            firstParsed.importantFields,
                            firstParsed.zeroFields
                        ) to firstParsed
                    )
                }
            }
        } ?.map {
            it.first.withoutTimeZoneOffset.millis to it.second.asFutureFor(it.first.dateTime).withoutTimeZoneOffset().millis
        } ?: emptyList()
    }

    @Transient
    val manualCheckDateTimes: List<CalculatedDateTime>? by lazy {
        manualCheckTime ?.parseDateTimes()
    }

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        val botWR = WeakReference(executor)

        val ratingPlugin = (pluginManager.plugins.firstOrNull {
            it is RatingPlugin
        } as? RatingPlugin) ?:let {
            commonLogger.warning(
                "Plugin $name was not correctly inited: can't get data about ratings"
            )
            return
        }

        val postsLikesTable = ratingPlugin.postsLikesTable
        val postsLikesMessagesTable = ratingPlugin.postsLikesMessagesTable

        postsLikesTable.ratingsChannel.subscribeChecking {
            botWR.get() ?.let {
                bot ->
                check(it, bot, baseConfig)
                true
            } ?: false
        }

        manualCheckDateTimes ?.let {
            GlobalScope.launch {
                while (isActive) {
                    it.executeNearFuture {
                        val botSR = botWR.get() ?: return@executeNearFuture null
                        val now = DateTime.now()
                        postsLikesMessagesTable.getEnabledPostsIdAndRatings().forEach {
                            pair ->
                            check(pair, botSR, baseConfig, now)
                        }
                    } ?.await() ?: break
                }
            }
        }
    }

    private suspend fun check(
        dataPair: PostIdRatingPair,
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        now: DateTime = DateTime.now()
    ) = PostsTable.getPostCreationDateTime(dataPair.first) ?.also {
        creatingDate ->
        check(dataPair, creatingDate, executor, baseConfig, now)
    }

    private suspend fun check(
        dataPair: PostIdRatingPair,
        creatingDate: DateTime,
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        now: DateTime = DateTime.now()
    ) {
        for (period in skipDateTime) {
            if (creatingDate.plus(period.second).isAfter(now) && creatingDate.plus(period.first).isBefore(now)) {
                return
            }
        }
        if (dataPair.second < minimalRate || PostsMessagesTable.getMessagesOfPost(dataPair.first).isEmpty()) {
            deletePost(
                executor,
                baseConfig.sourceChatId,
                dataPair.first
            )
        }
    }
}