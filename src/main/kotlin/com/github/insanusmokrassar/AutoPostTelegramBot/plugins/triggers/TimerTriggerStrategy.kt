package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.triggers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.nearDateTime
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.*
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
data class TimerStrategyConfig(
    private val delay: Long? = null,
    private val time: String = "00:00-00:00 01:00"
) {
    private var lastTime = DateTime.now()

    private val timesOfTriggering: List<CalculatedDateTime> by lazy {
        time.parseDateTimes()
    }
    val nextTriggerTime: DateTime?
        get() = delay ?.let {
            lastTime = lastTime.plus(it)
            lastTime
        } ?: timesOfTriggering.nearDateTime()
}

private val TimerTriggerStrategyScope = NewDefaultCoroutineScope(1)

@Serializable
class TimerTriggerStrategy (
    @Optional
    private val config: TimerStrategyConfig = TimerStrategyConfig()
) : Plugin {
    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val publisher = pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher
            ?: return
        val chooser: Chooser = pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser
            ?: return

        TimerTriggerStrategyScope.launch {
            while (isActive) {
                launch {
                    try {
                        chooser.triggerChoose().forEach {
                            publisher.publishPost(it)
                        }
                    } catch (e: Exception) {
                        commonLogger.throwing(
                            this@TimerTriggerStrategy::class.java.simpleName,
                            "Trigger of publishing",
                            e
                        )
                    }
                }
                delay(config.nextTriggerTime ?.millis ?.minus(System.currentTimeMillis()) ?: break)
            }
        }
    }
}
