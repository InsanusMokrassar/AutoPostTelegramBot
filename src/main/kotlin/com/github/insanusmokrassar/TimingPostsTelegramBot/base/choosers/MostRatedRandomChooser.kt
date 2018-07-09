package com.github.insanusmokrassar.TimingPostsTelegramBot.base.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsLikesTable
import java.util.*

class MostRatedRandomChooser : Chooser {
    private val random = Random()

    override fun triggerChoose(): Collection<Int> {
        return PostsLikesTable.getMostRated().let {
            if (it.isEmpty()) {
                it
            } else {
                listOf(
                    it[random.nextInt(it.size)]
                )
            }
        }
    }
}