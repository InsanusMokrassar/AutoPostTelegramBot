package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginVersion

class NoneChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return emptyList()
    }
}