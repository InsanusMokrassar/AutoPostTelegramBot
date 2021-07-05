package dev.inmo.AutoPostTelegramBot.plugins.choosers

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.tgbotapi.bot.RequestsExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.DateTime
import java.util.*

@Serializable
class MostRatedRandomChooser : RateChooser() {
    @Transient
    private val random = Random()
    @Transient
    private val mostRatedChooser = MostRatedChooser()

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        mostRatedChooser.onInit(executor, baseConfig, pluginManager)
    }

    override suspend fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        return mostRatedChooser.triggerChoose(time, exceptions).let {
            if (it.isEmpty()) {
                it
            } else {
                listOf(it.elementAt(random.nextInt(it.size)))
            }
        }
    }
}