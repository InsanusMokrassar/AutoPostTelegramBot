package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginVersion
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