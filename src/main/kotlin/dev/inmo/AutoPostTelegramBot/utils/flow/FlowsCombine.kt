package dev.inmo.AutoPostTelegramBot.utils.flow

import dev.inmo.micro_utils.coroutines.safelyWithoutExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.*

internal fun <T> combineFlows(vararg flows: Flow<T>, scope: CoroutineScope, bcCapacity: Int = 128): Flow<T> {
    val internalBCChannel = BroadcastChannel<T>(bcCapacity)
    flows.forEach {
        it.onEach {
            safelyWithoutExceptions {
                internalBCChannel.send(it)
            }
        }.launchIn(scope)
    }
    return internalBCChannel.asFlow()
}
