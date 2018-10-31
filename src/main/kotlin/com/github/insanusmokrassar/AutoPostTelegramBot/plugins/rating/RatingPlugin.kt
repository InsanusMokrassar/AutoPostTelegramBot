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
import com.pengrad.telegrambot.TelegramBot
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.ref.WeakReference

class RatingPlugin : Plugin {
    private var likeReceiver: LikeReceiver? = null
    private var dislikeReceiver: DislikeReceiver? = null
    private var disableReceiver: DisableReceiver? = null
    private var enableReceiver: EnableReceiver? = null

    private var registeredRefresher: RegisteredRefresher? = null

    private var availableRates: AvailableRates? = null
    private var mostRated: MostRated? = null

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
        val botWR = WeakReference(bot)

        (pluginManager.plugins.firstOrNull { it is BasePlugin } as? BasePlugin)?.also {
            postsLikesMessagesTable.postsUsedTablePluginName = it.postsUsedTable to name
        }

        likeReceiver ?: let {
            likeReceiver = LikeReceiver(bot, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        dislikeReceiver ?: let {
            dislikeReceiver = DislikeReceiver(bot, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }
        disableReceiver ?: let {
            disableReceiver = DisableReceiver(bot, baseConfig.sourceChatId, postsLikesMessagesTable)
        }
        enableReceiver ?: let {
            enableReceiver = EnableReceiver(bot, baseConfig.sourceChatId, postsLikesTable, postsLikesMessagesTable)
        }

        mostRated ?:let {
            mostRated = MostRated(botWR, postsLikesTable)
        }
        availableRates ?:let {
            availableRates = AvailableRates(botWR, postsLikesMessagesTable)
        }

        registeredRefresher = RegisteredRefresher(
            baseConfig.sourceChatId,
            bot,
            postsLikesTable,
            postsLikesMessagesTable
        )
    }

}
