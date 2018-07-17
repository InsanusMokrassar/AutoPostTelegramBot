package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.base.commands.deletePost
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostIdRatingPair
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

private data class GarbageCollectorConfig(
    val minimalRate: Int = -3,
    val manualCheckDelay: Long? = null
)

class GarbageCollector(
    params: IObject<Any>?
) : Plugin {
    override val version: PluginVersion = 0L

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
            pluginLogger.warning(
                "Plugin $name was not correctly inited: can't get data about ratings"
            )
            return
        }

        val postsLikesTable = ratingPlugin.postsLikesTable
        val postsLikesMessagesTable = ratingPlugin.postsLikesMessagesTable

        postsLikesTable.ratingsChannel.openSubscription().let {
            launch {
                while (isActive) {
                    val bot = botWR.get() ?: break
                    it.receive().let {
                        check(it, bot, baseConfig)
                    }
                }
                it.cancel()
            }
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
    ) {
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