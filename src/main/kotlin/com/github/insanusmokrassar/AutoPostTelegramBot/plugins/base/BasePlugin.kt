package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMediaGroup
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.*
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import java.lang.ref.WeakReference

@Serializable
class BasePlugin : Plugin {
    @Transient
    private var deletePost: DeletePost? = null
    @Transient
    private var startPost: StartPost? = null
    @Transient
    private var fixPost: FixPost? = null

    @Transient
    private var onMediaGroup: OnMediaGroup? = null
    @Transient
    private var onMessage: OnMessage? = null

    @Transient
    private var postMessagesRegistrant: PostMessagesRegistrant? = null

    @Transient
    private var renewRegisteredMessage: RenewRegisteredMessage? = null

    @Transient
    val postsUsedTable = PostsUsedTable()

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val botWR = WeakReference(executor)

        deletePost = DeletePost(
            botWR
        )
        startPost = StartPost()
        fixPost = FixPost(
            botWR
        )

        onMediaGroup = OnMediaGroup(baseConfig.sourceChatId)
        onMessage = OnMessage(baseConfig.sourceChatId)

        postMessagesRegistrant = PostMessagesRegistrant(
            executor,
            baseConfig.sourceChatId
        ).also {
            renewRegisteredMessage = RenewRegisteredMessage(it)
        }
    }
}
