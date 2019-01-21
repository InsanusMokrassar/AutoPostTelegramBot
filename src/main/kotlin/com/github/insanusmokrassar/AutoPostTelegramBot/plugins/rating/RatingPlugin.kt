package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.BasePlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands.AvailableRates
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.commands.MostRated
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers.*
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

@Serializable
class RatingPlugin : Plugin {
    @Transient
    private var likeReceiver: LikeReceiver? = null
    @Transient
    private var dislikeReceiver: DislikeReceiver? = null
    @Transient
    private var disableReceiver: DisableReceiver? = null
    @Transient
    private var enableReceiver: EnableReceiver? = null

    @Transient
    private var registeredRefresher: RegisteredRefresher? = null

    @Transient
    private var availableRates: AvailableRates? = null
    @Transient
    private var mostRated: MostRated? = null

    @Transient
    val postsLikesTable = PostsLikesTable()
    @Transient
    val postsLikesMessagesTable = PostsLikesMessagesTable(postsLikesTable).also {
        postsLikesTable.postsLikesMessagesTable = it
    }

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(postsLikesTable, postsLikesMessagesTable)
        }
    }

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        val botWR = WeakReference(executor)

        (pluginManager.plugins.firstOrNull { it is BasePlugin } as? BasePlugin)?.also {
            postsLikesMessagesTable.postsUsedTablePluginName = it.postsUsedTable to name
        }

        likeReceiver ?: let {
            likeReceiver = LikeReceiver(executor, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        dislikeReceiver ?: let {
            dislikeReceiver = DislikeReceiver(executor, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        disableReceiver ?: let {
            disableReceiver = DisableReceiver(executor, baseConfig.sourceChatId, postsLikesMessagesTable)
        }
        enableReceiver ?: let {
            enableReceiver = EnableReceiver(executor, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }

        mostRated ?:let {
            mostRated = MostRated(botWR, postsLikesTable)
        }
        availableRates ?:let {
            availableRates = AvailableRates(botWR, postsLikesMessagesTable)
        }

        registeredRefresher = RegisteredRefresher(
            baseConfig.sourceChatId,
            executor,
            postsLikesTable,
            postsLikesMessagesTable
        )
    }

}
