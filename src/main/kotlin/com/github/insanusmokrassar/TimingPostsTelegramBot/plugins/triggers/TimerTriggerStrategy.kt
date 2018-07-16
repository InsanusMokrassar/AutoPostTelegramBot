package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.triggers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.*
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.*

private data class TimerStrategyConfig(
    val delay: Long = 60 * 60 *1000
)

class TimerTriggerStrategy (
    config: IObject<Any>?
) : Plugin {
    override val version: PluginVersion = 0L

    private val config = config ?.toObject(TimerStrategyConfig::class.java) ?: TimerStrategyConfig()

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val publisher = pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher
            ?: return
        val chooser: Chooser = pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser
            ?: return

        launch {
            while (isActive) {
                synchronized(PostsTable) {
                    synchronized(PostsMessagesTable) {
                        synchronized(PostsLikesTable) {
                            try {
                                chooser.triggerChoose().forEach {
                                    publisher.publishPost(it)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                delay(config.delay)
            }
        }
    }
}
