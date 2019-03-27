package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
class MostRatedChooser : RateChooser() {
    override fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        return listOfNotNull(postsLikesTable ?.getMostRated() ?.minus(exceptions) ?.min())
    }
}