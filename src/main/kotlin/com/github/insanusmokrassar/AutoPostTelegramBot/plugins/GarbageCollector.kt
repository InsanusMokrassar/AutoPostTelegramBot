package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    private lateinit var ratingPlugin: RatingPlugin

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

    val manualCheckDateTimes: List<CalculatedDateTime>? by lazy {
        manualCheckTime ?.parseDateTimes()
    }

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        val botWR = WeakReference(executor)

        ratingPlugin = pluginManager.findFirstPlugin() ?:let {
            commonLogger.warning(
                "Plugin $name was not correctly inited: can't get data about ratings"
            )
            return
        }

        val postsTable = baseConfig.postsTable
        val postsMessagesTable = baseConfig.postsMessagesTable

        NewDefaultCoroutineScope(3).apply {
            launch {
                ratingPlugin.allocateRatingChangedFlow().collectWithErrors {
                    check(postsTable, postsMessagesTable, it, executor, baseConfig)
                }
            }

            manualCheckDateTimes ?.let {
                launch {
                    while (isActive) {
                        it.executeNearFuture {
                            val botSR = botWR.get() ?: return@executeNearFuture null
                            val now = DateTime.now()
                            ratingPlugin.getRegisteredPosts().flatMap {
                                ratingPlugin.getPostRatings(it)
                            }.forEach { pair ->
                                check(postsTable, postsMessagesTable, pair, botSR, baseConfig, now)
                            }
                        } ?.await() ?: break
                    }
                }
            }
        }
    }

    private suspend fun check(
        postsTable: PostsBaseInfoTable,
        postsMessagesTable: PostsMessagesInfoTable,
        dataPair: RatingPair,
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        now: DateTime = DateTime.now()
    ) = ratingPlugin.resolvePostId(dataPair.first) ?.let {
        postsTable.getPostCreationDateTime(it) ?.also { creatingDate ->
            check(postsTable, postsMessagesTable, dataPair, creatingDate, executor, baseConfig, now)
        }
    }

    private suspend fun check(
        postsTable: PostsBaseInfoTable,
        postsMessagesTable: PostsMessagesInfoTable,
        dataPair: RatingPair,
        creatingDate: DateTime,
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        now: DateTime = DateTime.now()
    ) {
        val postId = ratingPlugin.resolvePostId(dataPair.first) ?: return
        for (period in skipDateTime) {
            if (creatingDate.plus(period.second).isAfter(now) && creatingDate.plus(period.first).isBefore(now)) {
                return
            }
        }
        if (dataPair.second < minimalRate || postsMessagesTable.getMessagesOfPost(postId).isEmpty()) {
            deletePost(
                executor,
                baseConfig.sourceChatId,
                postId,
                postsTable,
                postsMessagesTable
            )
        }
    }
}