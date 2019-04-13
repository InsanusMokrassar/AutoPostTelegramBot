package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer

object ListSerializer: KSerializer<List<Plugin>> {
    private val realSerializer = ArrayListSerializer(
        PolymorphicSerializer(Plugin::class)
    )

    override val descriptor: SerialDescriptor
        get() = realSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<Plugin> = realSerializer.deserialize(
        decoder
    ).map { it as Plugin }

    override fun serialize(encoder: Encoder, obj: List<Plugin>) = realSerializer.serialize(
        encoder, obj
    )
}
