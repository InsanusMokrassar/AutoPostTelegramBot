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
    private var enableReceiver: EnableReceiver? = null

    private var registeredRefresher: RegisteredRefresher? = null

    val postsLikesTable = PostsLikesTable()
    val postsLikesMessagesTable = PostsLikesMessagesTable(postsLikesTable).also {
        postsLikesTable.postsLikesMessagesTable = it
    }

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(postsLikesTable, postsLikesMessagesTable)
        }
    }

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        likeReceiver ?: let {
            likeReceiver = LikeReceiver(bot, postsLikesTable)
        }
        dislikeReceiver ?: let {
            dislikeReceiver = DislikeReceiver(bot, postsLikesTable)
        }
        disableReceiver ?: let {
            disableReceiver = DisableReceiver(bot, baseConfig.sourceChatId, postsLikesMessagesTable)
        }
        enableReceiver ?: let {
            enableReceiver = EnableReceiver(bot, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }

        registeredRefresher = RegisteredRefresher(
            baseConfig.sourceChatId,
            bot,
            postsLikesTable,
            postsLikesMessagesTable
        )
    }

}
