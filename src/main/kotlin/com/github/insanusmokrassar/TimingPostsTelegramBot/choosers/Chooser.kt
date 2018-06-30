package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

interface Chooser {

    /**
     * Must return postIds for posting
     */
    fun triggerChoose(): Collection<Int>
}