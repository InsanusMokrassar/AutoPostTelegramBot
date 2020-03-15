package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.enableOnMediaGroupsCallback
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.callbacks.enableOnMessageCallback
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
    private var onMediaGroupJob: Job? = null
    @Transient
    private var onMessageJob: Job? = null

    @Transient
    private var postMessagesRegistrant: PostMessagesRegistrant? = null

    @Transient
    private var renewRegisteredMessage: RenewRegisteredMessage? = null

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        val botWR = WeakReference(executor)

        val scope = NewDefaultCoroutineScope(3)

        deleteCommandJob = deleteCommandJob ?: scope.enableDeletingOfPostsCommand(
            botWR,
            baseConfig.postsTable,
            baseConfig.postsMessagesTable
        )

        startPostJob = startPostJob ?: scope.enableStartPostCommand()
        fixPostJob = fixPostJob ?: scope.enableFixPostCommand()

        onMediaGroupJob = onMediaGroupJob ?: scope.enableOnMediaGroupsCallback(
            baseConfig.postsTable,
            baseConfig.postsMessagesTable
        )
        onMessageJob = onMessageJob ?: scope.enableOnMessageCallback(
            baseConfig.postsTable,
            baseConfig.postsMessagesTable
        )

        postMessagesRegistrant = PostMessagesRegistrant(
            executor,
            baseConfig.sourceChatId,
            baseConfig.postsTable,
            baseConfig.postsMessagesTable
        ).also {
            renewRegisteredMessage = RenewRegisteredMessage(it, baseConfig.postsMessagesTable).also { it.onInit(executor, baseConfig, pluginManager) }
        }
    }
}
