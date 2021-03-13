package dev.inmo.AutoPostTelegramBot.base.plugins.abstractions

import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import org.joda.time.DateTime

interface Chooser : Plugin {
    /**
     * Must return postIds for posting
     */
    suspend fun triggerChoose(time: DateTime = DateTime.now(), exceptions: List<PostId> = emptyList()): Collection<Int>
}
