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
import kotlinx.serialization.Transient
import org.joda.time.DateTime

private val TimerTriggerStrategyScope = NewDefaultCoroutineScope(1)

@Serializable
class TimerTriggerStrategy (
    @Optional
    private val delay: Long? = null,
    @Optional
    private val time: String = "00:00-00:00 01:00"
) : Plugin {
    @Transient
    private var lastTime = DateTime.now()

    @Transient
    private val timesOfTriggering: List<CalculatedDateTime> by lazy {
        time.parseDateTimes()
    }
    @Transient
    val nextTriggerTime: DateTime?
        get() = delay ?.let {
            lastTime = lastTime.plus(it)
            lastTime
        } ?: timesOfTriggering.nearDateTime()

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
                delay(nextTriggerTime ?.millis ?.minus(System.currentTimeMillis()) ?: break)
            }
        }
    }
}
