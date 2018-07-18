package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.pengrad.telegrambot.TelegramBot

abstract class RateCommand : CommandPlugin() {
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