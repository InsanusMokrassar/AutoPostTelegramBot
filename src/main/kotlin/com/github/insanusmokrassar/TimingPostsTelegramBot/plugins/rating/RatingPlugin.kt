package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

class RatingPlugin : Plugin {
    override val version: PluginVersion = 0L

    private var likeReceiver: LikeReceiver? = null
    private var dislikeReceiver: DislikeReceiver? = null
    private var disableReceiver: DisableReceiver? = null

    private var registeredRefresher: RegisteredRefresher? = null

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(PostsLikesTable, PostsLikesMessagesTable)
        }
    }

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
        disableReceiver ?: let {
            disableReceiver = DisableReceiver(bot, baseConfig.sourceChatId)
        }

        val sourceChatId = baseConfig.sourceChatId

        registeredRefresher = RegisteredRefresher(
            baseConfig.sourceChatId,
            bot
        )

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
