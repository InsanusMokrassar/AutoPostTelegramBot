package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.pengrad.telegrambot.TelegramBot

abstract class RateChooser : Chooser {
    protected var postsLikesTable: PostsLikesTable? = null
        private set

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(bot, baseConfig, pluginManager)
        postsLikesTable = (pluginManager.plugins.firstOrNull { it is RatingPlugin } as? RatingPlugin) ?.postsLikesTable
    }
}