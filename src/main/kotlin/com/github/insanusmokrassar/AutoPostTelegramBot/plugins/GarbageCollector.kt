package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CalculatedDateTime
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
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
    @Optional
    val minimalRate: Int = -3,
    @Optional
    private val skipTime: String? = null,
    @Optional
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

    override suspend fun onInit(bot: AutoPostTelegramBot) {
        val botWR = WeakReference(bot)

        val ratingPlugin = (bot.pluginManager.plugins.firstOrNull {
            it is RatingPlugin
        } as? RatingPlugin) ?:let {
            commonLogger.warning(
                "Plugin $name was not correctly inited: can't get data about ratings"
            )
            return
        }

        val postsLikesTable = ratingPlugin.postsLikesTable
        val postsLikesMessagesTable = ratingPlugin.postsLikesMessagesTable

        val config = bot.config

        postsLikesTable.ratingsChannel.subscribeChecking {
            botWR.get() ?.let {
                bot ->
                check(it, bot)
                true
            } ?: false
        }

        manualCheckDateTimes ?.let {
            GlobalScope.launch {
                while (isActive) {
                    it.executeNearFuture {
                        val bot = botWR.get() ?: return@executeNearFuture null
                        val now = DateTime.now()
                        postsLikesMessagesTable.getEnabledPostsIdAndRatings().forEach {
                            pair ->
                            check(pair, bot, now)
                        }
                    } ?.await() ?: break
                }
            }
        }
    }

    private suspend fun check(
        dataPair: PostIdRatingPair,
        bot: AutoPostTelegramBot,
        now: DateTime = DateTime.now()
    ) = bot.postsTable.getPostCreationDateTime(dataPair.first) ?.also {
        creatingDate ->
        check(dataPair, creatingDate, bot, now)
    }

    private suspend fun check(
        dataPair: PostIdRatingPair,
        creatingDate: DateTime,
        bot: AutoPostTelegramBot,
        now: DateTime = DateTime.now()
    ) {
        for (period in skipDateTime) {
            if (creatingDate.plus(period.second).isAfter(now) && creatingDate.plus(period.first).isBefore(now)) {
                return
            }
        }
        if (dataPair.second < minimalRate || bot.postsMessagesTable.getMessagesOfPost(dataPair.first).isEmpty()) {
            deletePost(
                bot.executor,
                bot.config.sourceChatId,
                dataPair.first
            )
        }
    }
}