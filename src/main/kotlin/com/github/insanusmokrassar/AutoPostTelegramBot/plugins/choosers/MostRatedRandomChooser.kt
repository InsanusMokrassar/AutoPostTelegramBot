package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import java.util.*

class MostRatedRandomChooser : RateChooser() {
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