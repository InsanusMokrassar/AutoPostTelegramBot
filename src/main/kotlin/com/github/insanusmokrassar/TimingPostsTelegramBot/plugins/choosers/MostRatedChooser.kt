package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion

class MostRatedChooser : Chooser {
    override val version: PluginVersion = 0L

    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(PostsLikesTable.getMostRated().min())
    }
}