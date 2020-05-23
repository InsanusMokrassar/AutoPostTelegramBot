package com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow

import com.github.insanusmokrassar.TelegramBotAPI.utils.handleSafely
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.*

internal fun <T> combineFlows(vararg flows: Flow<T>, scope: CoroutineScope, bcCapacity: Int = 128): Flow<T> {
    val internalBCChannel = BroadcastChannel<T>(bcCapacity)
    flows.forEach {
        it.onEach {
            handleSafely({}) {
                internalBCChannel.send(it)
            }
        }.launchIn(scope)
    }
    return internalBCChannel.asFlow()
}
