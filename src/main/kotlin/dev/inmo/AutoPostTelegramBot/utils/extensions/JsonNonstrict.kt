package dev.inmo.AutoPostTelegramBot.utils.extensions

import kotlinx.serialization.json.Json

val JsonNonstrict = Json {
    ignoreUnknownKeys = false
}
val (Json.Default).nonstrict
    get() = JsonNonstrict
