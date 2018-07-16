package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable

class MostRatedChooser : Chooser {
    override val version: PluginVersion = 0L

    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(PostsLikesTable.getMostRated().min())
    }
}