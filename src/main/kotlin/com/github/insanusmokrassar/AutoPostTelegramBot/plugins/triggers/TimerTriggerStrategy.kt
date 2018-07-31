package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.pengrad.telegrambot.TelegramBot
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

private data class TimerStrategyConfig(
    val delay: Long = 60 * 60 *1000
)

class TimerTriggerStrategy (
    config: IObject<Any>?
) : Plugin {
    private val config = config ?.toObject(TimerStrategyConfig::class.java) ?: TimerStrategyConfig()

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val publisher = pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher
            ?: return
        val chooser: Chooser = pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser
            ?: return

        launch {
            while (isActive) {
                val nextTriggerTime = System.currentTimeMillis() + config.delay
                launch {
                    synchronized(PostsTable) {
                        synchronized(PostsMessagesTable) {
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
                delay(nextTriggerTime - System.currentTimeMillis())
            }
        }
    }
}
