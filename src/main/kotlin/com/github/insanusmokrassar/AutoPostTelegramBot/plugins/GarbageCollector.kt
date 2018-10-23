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
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.lang.ref.WeakReference

private val zeroDateTime: DateTime by lazy {
    DateTime(0, DateTimeZone.UTC)
}

private data class GarbageCollectorConfig(
    val minimalRate: Int = -3,
    private val skipTime: String? = null,
    private val manualCheckTime: String? = null
) {
    val skipDateTime: List<CalculatedPeriod> by lazy {
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
        } ?: emptyList()
    }

    val manualCheckDateTimes: List<CalculatedDateTime>? by lazy {
        manualCheckTime ?.parseDateTimes()
    }
}

class GarbageCollector(
    params: IObject<Any>?
) : Plugin {
    private val config = params ?.toObject(GarbageCollectorConfig::class.java) ?: GarbageCollectorConfig()

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        val botWR = WeakReference(bot)

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

        config.manualCheckDateTimes ?.let {
            launch {
                while (isActive) {
                    it.executeNearFuture {
                        val botSR = botWR.get() ?: return@executeNearFuture null
                        postsLikesMessagesTable.getEnabledPostsIdAndRatings().forEach {
                            pair ->
                            check(pair, botSR, baseConfig)
                        }
                    } ?.await() ?: break
                }
            }
        }
    }

    private fun check(
        dataPair: PostIdRatingPair,
        bot: TelegramBot,
        baseConfig: FinalConfig
    ) = PostsTable.getPostCreationDateTime(dataPair.first) ?.also {
        creatingDate ->
        check(dataPair, creatingDate, bot, baseConfig)
    }

    private fun check(
        dataPair: PostIdRatingPair,
        creatingDate: DateTime,
        bot: TelegramBot,
        baseConfig: FinalConfig
    ) {
        for (period in config.skipDateTime) {
            if (period.isBetween(creatingDate)) {
                return
            }
        }
        if (dataPair.second < config.minimalRate || PostsMessagesTable.getMessagesOfPost(dataPair.first).isEmpty()) {
            deletePost(
                bot,
                baseConfig.sourceChatId,
                baseConfig.logsChatId,
                dataPair.first
            )
        }
    }
}