package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import kotlinx.serialization.Serializable

@Serializable
class NoneChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return emptyList()
    }
}