package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
class MostRatedRandomChooser : RateChooser() {
    @Transient
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