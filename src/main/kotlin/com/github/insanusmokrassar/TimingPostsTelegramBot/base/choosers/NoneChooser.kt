package com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers

class NoneChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return emptyList()
    }
}