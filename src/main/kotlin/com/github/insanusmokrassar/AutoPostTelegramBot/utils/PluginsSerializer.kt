package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer

object ListSerializer: KSerializer<List<Plugin>> {
    private val realSerializer = ArrayListSerializer(
        PolymorphicSerializer
    )

    override val descriptor: SerialDescriptor
        get() = realSerializer.descriptor

    override fun deserialize(
        input: Decoder
    ): List<Plugin> = realSerializer.deserialize(
        input
    ).map { it as Plugin }

    override fun serialize(output: Encoder, obj: List<Plugin>) = realSerializer.serialize(
        output, obj
    )
}
