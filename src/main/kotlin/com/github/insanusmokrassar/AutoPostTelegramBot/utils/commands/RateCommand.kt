package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor

@Deprecated("Since the RatingPlugin is abstraction, this usage must be avoided")
abstract class RateCommand : CommandPlugin() {
    protected var postsLikesTable: PostsLikesTable? = null
        private set
    protected var postsLikesMessagesTable: PostsLikesMessagesTable? = null
        private set

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(executor, baseConfig, pluginManager)

        (pluginManager.plugins.firstOrNull { it is RatingPlugin } as? RatingPlugin) ?.also {
            postsLikesTable = it.postsLikesTable
            postsLikesMessagesTable = it.postsLikesMessagesTable
        }
    }
}