package com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin

interface Chooser : Plugin {
    /**
     * Must return postIds for posting
     */
    fun triggerChoose(): Collection<Int>
}
