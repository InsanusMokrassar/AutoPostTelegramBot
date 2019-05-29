package com.github.insanusmokrassar.AutoPostTelegramBot.plugins

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable
class RatingTimerAutoDisablePlugin : Plugin {
    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val ratingPlugin: MutableRatingPlugin = pluginManager.findFirstPlugin() ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that rating plugin was not found")
            return
        }

        val schedulerPlugin: SchedulerPlugin = pluginManager.findFirstPlugin() ?:let {
            commonLogger.warning("Plugin $name was not load for the reason that scheduler plugin was not found")
            return
        }

        CoroutineScope(Dispatchers.Default).apply {
            launch {
                schedulerPlugin.timerSchedulesTable.postTimeRegisteredFlow.collectWithErrors(
                    { _, error ->
                        commonLogger.throwing(
                            name,
                            "register post time scheduler registered",
                            error
                        )
                    }
                ) {
                    disableLikesForPost(
                        it.first,
                        ratingPlugin
                    )
                }
            }
            launch {
                ratingPlugin.allocateRatingAddedFlow().collectWithErrors {
                    schedulerPlugin.timerSchedulesTable.unregisterPost(it.first)
                }
            }
        }
    }
}