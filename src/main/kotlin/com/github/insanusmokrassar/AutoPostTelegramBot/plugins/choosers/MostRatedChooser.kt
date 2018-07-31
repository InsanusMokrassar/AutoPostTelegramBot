package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginVersion

class MostRatedChooser : RateChooser() {
    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.min())
    }
}