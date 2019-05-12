package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.Chooser
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
class NoneChooser : Chooser {
    override suspend fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        return emptyList()
    }
}