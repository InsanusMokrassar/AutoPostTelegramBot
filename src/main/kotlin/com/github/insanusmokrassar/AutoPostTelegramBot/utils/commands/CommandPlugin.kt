package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.AutoPostTelegramBot
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor

import java.lang.ref.WeakReference

abstract class CommandPlugin : Command(), Plugin {
    protected var botWR: WeakReference<RequestsExecutor>? = null

    override suspend fun onInit(bot: AutoPostTelegramBot) {
        botWR = WeakReference(bot.executor)
    }
}