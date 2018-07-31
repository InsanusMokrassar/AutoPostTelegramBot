package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.*
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMediaGroup
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.*
import com.pengrad.telegrambot.TelegramBot
import java.lang.ref.WeakReference

class BasePlugin : Plugin {
    private var deletePost: DeletePost? = null
    private var startPost: StartPost? = null
    private var fixPost: FixPost? = null

    private var onMediaGroup: OnMediaGroup? = null
    private var onMessage: OnMessage? = null

    private var defaultPostRegisteredMessage: DefaultPostRegisteredMessage? = null

    val postsUsedTable = PostsUsedTable()

    override fun onInit(bot: TelegramBot, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val botWR = WeakReference(bot)

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
            bot,
            baseConfig.sourceChatId
        )
    }
}
