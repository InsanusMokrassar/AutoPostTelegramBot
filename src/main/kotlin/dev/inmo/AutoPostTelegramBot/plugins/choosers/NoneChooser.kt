package dev.inmo.AutoPostTelegramBot.plugins.choosers

import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
class NoneChooser : Chooser {
    override suspend fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        return emptyList()
    }
}