package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import java.util.*

class MostRatedRandomChooser : RateChooser() {
    override val version: PluginVersion = 0L
    private val random = Random()

    override fun triggerChoose(): Collection<Int> {
        return postsLikesTable ?.getMostRated() ?.let {
            if (it.isEmpty()) {
                it
            } else {
                listOf(
                    it[random.nextInt(it.size)]
                )
            }
        } ?: emptyList()
    }
}