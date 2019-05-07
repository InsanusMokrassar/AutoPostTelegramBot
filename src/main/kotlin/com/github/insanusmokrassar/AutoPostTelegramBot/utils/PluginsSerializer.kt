package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.TelegramBotAPI.utils.toJson
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.*

object PluginSerializer : KSerializer<Plugin> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName(Plugin::class.simpleName ?: "Plugin")

    override fun serialize(encoder: Encoder, obj: Plugin) {
        val array = JsonArray(
            listOf(
                JsonPrimitive(obj::class.java.canonicalName),
                obj.toJson(serializerByTypeToken(obj::class.java))
            )
        )
        JsonArraySerializer.serialize(
            encoder,
            array
        )
    }

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

object PluginsListSerializer: KSerializer<List<Plugin>> by ArrayListSerializer(PluginSerializer)
