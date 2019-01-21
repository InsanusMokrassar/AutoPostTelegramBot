package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import kotlinx.serialization.Serializable

@Serializable
class MostRatedChooser : RateChooser() {
    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.min())
    }
}