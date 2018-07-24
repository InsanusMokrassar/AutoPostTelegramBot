package com.github.insanusmokrassar.AutoPostTelegramBot.base.models

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginConfig
import org.h2.Driver
import java.net.URL
import java.net.URLClassLoader

class Config (
    val targetChatId: Long? = null,
    val sourceChatId: Long? = null,
    val logsChatId: Long? = null,
    val botToken: String? = null,
    val databaseConfig: DatabaseConfig = DatabaseConfig(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        Driver::class.java.canonicalName,
        "sa",
        ""
    ),
    val proxy: ProxySettings? = null,
    val pluginsURL: String? = null,
    val plugins: List<PluginConfig> = emptyList(),
    val debug: Boolean = false
) {
    val finalConfig: FinalConfig
        @Throws(IllegalArgumentException::class)
        get() = FinalConfig(
            targetChatId ?: throw IllegalArgumentException("Target chat id (field \"targetChatId\") can't be null"),
            sourceChatId ?: throw IllegalArgumentException("Source chat id (field \"sourceChatId\") can't be null"),
            logsChatId ?: sourceChatId,
            botToken ?: throw IllegalArgumentException("Bot token (field \"botToken\") can't be null"),
            databaseConfig,
            proxy,
            plugins,
            debug,
            pluginsURL ?.let {
                arrayOf<ClassLoader>(
                    URLClassLoader.newInstance(arrayOf(URL(it)))
                )
            } ?: emptyArray()
        )
}

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)

class FinalConfig (
    val targetChatId: Long,
    val sourceChatId: Long,
    val logsChatId: Long,
    val botToken: String,
    val databaseConfig: DatabaseConfig,
    val proxy: ProxySettings? = null,
    val pluginsConfigs: List<PluginConfig> = emptyList(),
    val debug: Boolean = false,
    val additionalClassLoaders: Array<ClassLoader> = emptyArray()
)
