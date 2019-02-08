package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor


abstract class RateChooser : Chooser {
    protected var postsLikesTable: PostsLikesTable? = null
        private set

    override suspend fun onInit(bot: AutoPostTelegramBot) {
        super.onInit(bot)
        postsLikesTable = (bot.pluginManager.plugins.firstOrNull { it is RatingPlugin } as? RatingPlugin) ?.postsLikesTable
    }
}