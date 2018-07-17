package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.pengrad.telegrambot.TelegramBot

abstract class RateCommand : Command() {
    protected var postsLikesTable: PostsLikesTable? = null
        private set
    protected var postsLikesMessagesTable: PostsLikesMessagesTable? = null
        private set

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(bot, baseConfig, pluginManager)

        (pluginManager.plugins.firstOrNull { it is RatingPlugin } as? RatingPlugin) ?.also {
            postsLikesTable = it.postsLikesTable
            postsLikesMessagesTable = it.postsLikesMessagesTable
        }
    }
}