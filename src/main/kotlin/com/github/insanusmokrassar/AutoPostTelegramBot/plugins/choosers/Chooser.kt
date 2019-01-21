package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin


interface Chooser : Plugin {
    /**
     * Must return postIds for posting
     */
    fun triggerChoose(): Collection<Int>
}