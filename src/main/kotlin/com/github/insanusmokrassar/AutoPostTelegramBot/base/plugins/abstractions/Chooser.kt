package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import org.joda.time.DateTime

interface Chooser : Plugin {
    /**
     * Must return postIds for posting
     */
    fun triggerChoose(time: DateTime = DateTime.now(), exceptions: List<PostId> = emptyList()): Collection<Int>
}
