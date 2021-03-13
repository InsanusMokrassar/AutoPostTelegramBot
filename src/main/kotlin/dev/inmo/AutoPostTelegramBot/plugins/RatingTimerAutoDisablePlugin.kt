package dev.inmo.AutoPostTelegramBot.plugins

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.*
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import dev.inmo.AutoPostTelegramBot.plugins.scheduler.SchedulerPlugin
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
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
                schedulerPlugin.getSchedulesTable().postTimeRegisteredFlow.collectWithErrors(
                    { _, error ->
                        commonLogger.throwing(
                            name,
                            "register post time scheduler registered",
                            error
                        )
                    }
                ) { (postId, _) ->
                    ratingPlugin.getPostRatings(postId).forEach { (ratingId, _) ->
                        ratingPlugin.deleteRating(ratingId)
                    }
                }
            }
            launch {
                ratingPlugin.allocateRatingAddedFlow().collectWithErrors {
                    schedulerPlugin.getSchedulesTable().unregisterPost(it.first)
                }
            }
        }
    }
}