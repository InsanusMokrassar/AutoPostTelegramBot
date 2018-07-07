package com.github.insanusmokrassar.TimingPostsTelegramBot

import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.ChooserConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.choosers.choosers
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.Plugin
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.PluginConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.ProxySettings
import org.h2.Driver

class Config (
    val targetChatId: String? = null,
    val sourceChatId: String? = null,
    val botToken: String? = null,
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    val chooser: ChooserConfig = ChooserConfig(
        choosers.keys.first()
    ),
    val proxy: ProxySettings? = null,
    val plugins: List<PluginConfig> = emptyList(),
    val debug: Boolean = false
) {
    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId ?: throw IllegalArgumentException("Target chat id (field \"targetChatId\") can't be null"),
            sourceChatId ?: throw IllegalArgumentException("Source chat id (field \"sourceChatId\") can't be null"),
            botToken ?: throw IllegalArgumentException("Bot token (field \"botToken\") can't be null"),
            databaseConfig,
            chooser,
            proxy,
            plugins.mapNotNull { it.newInstance() },
            debug
        )
}

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)

class FinalConfig (
    val targetChatId: String,
    val sourceChatId: String,
    val botToken: String,
    val databaseConfig: DatabaseConfig,
    val chooser: ChooserConfig,
    val proxy: ProxySettings? = null,
    val plugins: List<Plugin> = emptyList(),
    val debug: Boolean = false
)
