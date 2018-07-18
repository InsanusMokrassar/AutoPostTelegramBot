package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.pengrad.telegrambot.TelegramBot

abstract class RateChooser : Chooser {
    protected var postsLikesTable: PostsLikesTable? = null
        private set

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(bot, baseConfig, pluginManager)
        postsLikesTable = (pluginManager.plugins.firstOrNull { it is RatingPlugin } as? RatingPlugin) ?.postsLikesTable
    }
}