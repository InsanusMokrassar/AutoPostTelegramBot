package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.DateTime
import java.util.*

@Serializable
class MostRatedRandomChooser : RateChooser() {
    @Transient
    private val random = Random()

    override fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        return postsLikesTable ?.getMostRated() ?.minus(exceptions) ?.let {
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