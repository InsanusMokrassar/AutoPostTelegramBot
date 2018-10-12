package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import java.lang.ref.WeakReference

private data class GarbageCollectorConfig(
    val minimalRate: Int = -3,
    val trackingDelay: Long? = null,
    val manualCheckDelay: Long? = null
)

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

        config.manualCheckDelay ?.let {
            launch {
                while (isActive) {
                    val bot = botWR.get() ?: break
                    postsLikesMessagesTable.getEnabledPostsIdAndRatings().forEach {
                        check(it, bot, baseConfig)
                    }
                    delay(it)
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
        if (
            dataPair.second < config.minimalRate || PostsMessagesTable.getMessagesOfPost(dataPair.first).isEmpty()
            && creatingDate.plus(config.trackingDelay ?: 0).isBeforeNow
        ) {
            deletePost(
                bot,
                baseConfig.sourceChatId,
                baseConfig.logsChatId,
                dataPair.first
            )
        }
    }
}