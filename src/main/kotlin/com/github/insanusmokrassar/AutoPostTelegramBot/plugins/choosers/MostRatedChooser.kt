package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginVersion

class MostRatedChooser : RateChooser() {
    override val version: PluginVersion = 0L

    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.min())
    }
}