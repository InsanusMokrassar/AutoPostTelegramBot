package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMediaGroup
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.*
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor

import java.lang.ref.WeakReference

class BasePlugin : Plugin {
    private var deletePost: DeletePost? = null
    private var startPost: StartPost? = null
    private var fixPost: FixPost? = null

    private var onMediaGroup: OnMediaGroup? = null
    private var onMessage: OnMessage? = null

    private var defaultPostRegisteredMessage: DefaultPostRegisteredMessage? = null

    val postsUsedTable = PostsUsedTable()

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val botWR = WeakReference(executor)

        deletePost = DeletePost(
            baseConfig.logsChatId,
            botWR
        )
        startPost = StartPost()
        fixPost = FixPost(
            botWR
        )

        onMediaGroup = OnMediaGroup(baseConfig.sourceChatId)
        onMessage = OnMessage(baseConfig.sourceChatId)

        defaultPostRegisteredMessage = DefaultPostRegisteredMessage(
            executor,
            baseConfig.sourceChatId
        )
    }
}
