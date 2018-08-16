package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

class NoneChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return emptyList()
    }
}