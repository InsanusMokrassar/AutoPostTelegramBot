package com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers

interface Chooser {

    /**
     * Must return postIds for posting
     */
    fun triggerChoose(): Collection<Int>
}