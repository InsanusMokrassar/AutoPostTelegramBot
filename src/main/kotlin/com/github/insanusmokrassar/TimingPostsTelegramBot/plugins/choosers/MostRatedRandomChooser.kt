package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import java.util.*

class MostRatedRandomChooser : Chooser {
    override val version: PluginVersion = 0L
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