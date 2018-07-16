package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.refreshRegisteredMessage
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

class RatingPlugin : Plugin {
    override val version: PluginVersion = 0L

    private var likeReceiver: LikeReceiver? = null
    private var dislikeReceiver: DislikeReceiver? = null

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        likeReceiver ?: let {
            likeReceiver = LikeReceiver(bot)
        }
        dislikeReceiver ?: let {
            dislikeReceiver = DislikeReceiver(bot)
        }

        val sourceChatId = baseConfig.sourceChatId

        val botWR = WeakReference(bot)
        PostsLikesTable.ratingsChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val bot = botWR.get() ?: break
                    val update = it.receive()
                    refreshRegisteredMessage(
                        sourceChatId,
                        bot,
                        update.first,
                        update.second
                    )
                }
                it.cancel()
            }
        }
        PostTransactionTable.transactionCompletedChannel.openSubscription().also {
            launch {
                while (isActive) {
                    val bot = botWR.get() ?: break
                    val postId = it.receive()
                    refreshRegisteredMessage(
                        sourceChatId,
                        bot,
                        postId
                    )
                }
                it.cancel()
            }
        }
    }

}
