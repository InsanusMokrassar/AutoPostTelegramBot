package com.github.insanusmokrassar.TimingPostsTelegramBot.utils.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.pengrad.telegrambot.TelegramBot
import java.lang.ref.WeakReference

abstract class CommandPlugin : Command(), Plugin {
    protected var botWR: WeakReference<TelegramBot>? = null

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(bot)
    }
}