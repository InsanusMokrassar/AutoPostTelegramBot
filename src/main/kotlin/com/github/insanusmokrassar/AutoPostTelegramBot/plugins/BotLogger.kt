package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.BotConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.initHandler
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.pengrad.telegrambot.TelegramBot

class BotLogger(
    params: IObject<Any>? = null
) : Plugin {
    val config = params ?.toObject(BotConfig::class.java)

    constructor(): this(null)

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(bot, baseConfig, pluginManager)
        val logBot = config ?.createBot() ?: bot
        initHandler(logBot, baseConfig.logsChatId)
    }
}