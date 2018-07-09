package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.builtin.commands.deletePost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.publishers.Publisher
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
    private val config = params ?.toObject(GarbageCollectorConfig::class.java) ?: GarbageCollectorConfig()

    override fun init(
        baseConfig: FinalConfig,
        chooser: Chooser,
        publisher: Publisher,
        bot: TelegramBot
    ) {
        val botWR = WeakReference(bot)

        PostsLikesTable.subscribeChannel.openSubscription().let {
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
                    PostsTable.getAll().map {
                        PostIdRatingPair(it, PostsLikesTable.getPostRating(it))
                    }.forEach {
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