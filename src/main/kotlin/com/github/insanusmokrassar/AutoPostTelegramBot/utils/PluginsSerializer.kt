package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TelegramBotAPI.utils.toJson
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

@Serializer(Plugin::class)
internal object PluginSerializer : KSerializer<Plugin> {
    override val descriptor: SerialDescriptor = JsonArraySerializer.descriptor

    override fun serialize(encoder: Encoder, value: Plugin) {
        val array = JsonArray(
            listOf(
                JsonPrimitive(value::class.java.canonicalName),
                value.toJson(serializerByTypeToken(value::class.java))
            )
        )
        JsonArraySerializer.serialize(
            encoder,
            array
        )
    }

    @ImplicitReflectionSerializer
    override fun deserialize(decoder: Decoder): Plugin {
        val decoded = decoder.decode(JsonArraySerializer)
        val expectedClass = decoded.getPrimitive(0).content
        val expectedArgumentObject = decoded.getObjectOrNull(1)
        val serializer = Class.forName(expectedClass).kotlin.serializer()
        val argumentsObject = expectedArgumentObject ?.toString() ?: JsonObject(emptyMap()).toString()
        return Json.nonstrict.parse(
            serializer,
            argumentsObject
        ) as Plugin
    }
}

object PluginsListSerializer: KSerializer<List<Plugin>> by ListSerializer(PluginSerializer)
