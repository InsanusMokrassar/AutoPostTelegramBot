package dev.inmo.AutoPostTelegramBot.utils

import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin
import dev.inmo.AutoPostTelegramBot.utils.extensions.nonstrict
import dev.inmo.tgbotapi.utils.toJson
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializer(Plugin::class)
internal object PluginSerializer : KSerializer<Plugin> {
    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

    @InternalSerializationApi
    override fun serialize(encoder: Encoder, value: Plugin) {
        val array = JsonArray(
            listOf(
                JsonPrimitive(value::class.java.canonicalName),
                value.toJson(value::class.serializer() as KSerializer<Plugin>)
            )
        )
        JsonArray.serializer().serialize(
            encoder,
            array
        )
    }

    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): Plugin {
        val decoded = decoder.decodeSerializableValue(JsonArray.serializer())
        val expectedClass = decoded[0].jsonPrimitive.content
        val expectedArgumentObject = decoded.getOrNull(1) as? JsonObject
        val serializer = Class.forName(expectedClass).kotlin.serializer()
        val argumentsObject = expectedArgumentObject ?.toString() ?: JsonObject(emptyMap()).toString()
        return Json.nonstrict.decodeFromString(
            serializer,
            argumentsObject
        ) as Plugin
    }
}

object PluginsListSerializer: KSerializer<List<Plugin>> by ListSerializer(PluginSerializer)
