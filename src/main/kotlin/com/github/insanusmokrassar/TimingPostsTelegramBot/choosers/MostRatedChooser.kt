package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable

class MostRatedChooser : Chooser {
    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(PostsLikesTable.getMostRated().min())
    }
}