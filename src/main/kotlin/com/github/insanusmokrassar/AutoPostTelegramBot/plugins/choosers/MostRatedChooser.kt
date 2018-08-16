package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

class MostRatedChooser : RateChooser() {
    override fun triggerChoose(): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.min())
    }
}