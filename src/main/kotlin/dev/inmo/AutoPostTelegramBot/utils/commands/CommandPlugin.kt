package dev.inmo.AutoPostTelegramBot.utils.commands

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.tgbotapi.bot.RequestsExecutor

import java.lang.ref.WeakReference

abstract class CommandPlugin : Command(), Plugin {
    protected var botWR: WeakReference<RequestsExecutor>? = null

    override suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        botWR = WeakReference(executor)
    }
}