package com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable

class MostRatedChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(PostsLikesTable.getMostRated().min())
    }
}