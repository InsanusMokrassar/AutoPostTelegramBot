package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class DefaultPluginManager(
    pluginsCollection: Collection<Plugin>
) : PluginManager {
    override val plugins: List<Plugin> = pluginsCollection.toList()
    constructor(pluginsConfigs: List<PluginConfig>) : this(
        pluginsConfigs.mapNotNull {
            it.newInstance() ?.also {
                pluginLogger.info("Plugin ${it.name} instantiated")
            }
        }
    )

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig) {
        runBlocking {
            plugins.map {
                launch {
                    it.onInit(
                        bot,
                        baseConfig,
                        this@DefaultPluginManager
                    )
                    pluginLogger.info("Plugin ${it.name} was init")
                }
            }.forEach {
                it.join()
            }
        }
    }
}