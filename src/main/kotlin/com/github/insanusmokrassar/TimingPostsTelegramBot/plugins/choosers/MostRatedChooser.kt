package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable

class MostRatedChooser : RateChooser() {
    override val version: PluginVersion = 0L

    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.min())
    }
}