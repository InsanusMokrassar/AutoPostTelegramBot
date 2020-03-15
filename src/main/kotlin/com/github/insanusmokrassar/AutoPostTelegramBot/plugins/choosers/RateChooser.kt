package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.findFirstPlugin
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor


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