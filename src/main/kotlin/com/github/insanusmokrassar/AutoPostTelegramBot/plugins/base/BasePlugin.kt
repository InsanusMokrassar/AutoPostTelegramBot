package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMediaGroup
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.OnMessage
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import java.lang.ref.WeakReference

@Serializable
class BasePlugin : Plugin {
    @Transient
    private var deleteCommandJob: Job? = null
    @Transient
    private var startPostJob: Job? = null
    @Transient
    private var fixPostJob: Job? = null

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

        val scope = NewDefaultCoroutineScope(3)

        deleteCommandJob = deleteCommandJob ?: scope.enableDeletingOfPostsCommand(botWR)

        startPostJob = startPostJob ?: scope.enableStartPostCommand()
        fixPostJob = fixPostJob ?: scope.enableFixPostCommand()

        onMediaGroup = OnMediaGroup(baseConfig.sourceChatId)
        onMessage = OnMessage(baseConfig.sourceChatId)

        postMessagesRegistrant = PostMessagesRegistrant(
            executor,
            baseConfig.sourceChatId
        ).also {
            renewRegisteredMessage = RenewRegisteredMessage(it).also { it.onInit(executor, baseConfig, pluginManager) }
        }
    }
}
