package dev.inmo.AutoPostTelegramBot.plugins.choosers

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import dev.inmo.AutoPostTelegramBot.base.plugins.findFirstPlugin
import dev.inmo.tgbotapi.bot.RequestsExecutor


abstract class RateChooser : Chooser {
    protected lateinit var ratingPlugin: RatingPlugin
        private set

    protected lateinit var postsTable: PostsBaseInfoTable
    protected lateinit var postsMessagesTable: PostsMessagesInfoTable

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(executor, baseConfig, pluginManager)
        ratingPlugin = pluginManager.findFirstPlugin() ?: return
        postsTable = baseConfig.postsTable
        postsMessagesTable = baseConfig.postsMessagesTable
    }
}