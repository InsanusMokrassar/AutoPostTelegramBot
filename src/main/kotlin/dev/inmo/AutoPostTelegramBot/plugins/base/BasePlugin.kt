package dev.inmo.AutoPostTelegramBot.plugins.base

import dev.inmo.AutoPostTelegramBot.base.models.FinalConfig
import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import dev.inmo.AutoPostTelegramBot.base.plugins.PluginManager
import dev.inmo.AutoPostTelegramBot.plugins.base.callbacks.enableOnMediaGroupsCallback
import dev.inmo.AutoPostTelegramBot.plugins.base.callbacks.enableOnMessageCallback
import dev.inmo.AutoPostTelegramBot.plugins.base.commands.*
import dev.inmo.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import dev.inmo.tgbotapi.bot.RequestsExecutor
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

        postMessagesRegistrant = PostMessagesRegistrant(
            executor,
            baseConfig.sourceChatId,
            baseConfig.postsTable,
            baseConfig.postsMessagesTable
        ).also {
            renewRegisteredMessage = RenewRegisteredMessage(it, baseConfig.postsMessagesTable).also { it.onInit(executor, baseConfig, pluginManager) }
        }

        onMediaGroupJob = onMediaGroupJob ?: scope.enableOnMediaGroupsCallback(
            CommonKnownPostsTransactions
        )
        onMessageJob = onMessageJob ?: scope.enableOnMessageCallback(
            CommonKnownPostsTransactions
        )
    }
}
