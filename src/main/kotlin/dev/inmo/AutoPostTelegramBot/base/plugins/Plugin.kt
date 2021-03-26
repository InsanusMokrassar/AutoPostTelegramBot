package dev.inmo.AutoPostTelegramBot.base.plugins

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.utils.PluginSerializer
import dev.inmo.tgbotapi.bot.RequestsExecutor
import kotlinx.serialization.Serializable

import java.util.logging.Level
import java.util.logging.Logger

typealias PluginName = String

val commonLogger = Logger.getLogger("common").also { logger ->
    logger.level = Level.FINER
    logger.handlers.forEach {
        it.level = logger.level
    }
}

@Serializable(PluginSerializer::class)
interface Plugin {
    val name: PluginName
        get() = this::class.java.simpleName

    suspend fun onInit(
        executor: RequestsExecutor,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) { }
}