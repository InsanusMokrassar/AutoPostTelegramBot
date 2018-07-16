package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion

class NoneChooser : Chooser {
    override val version: PluginVersion = 0L
    override fun triggerChoose(): Collection<Int> {
        return emptyList()
    }
}