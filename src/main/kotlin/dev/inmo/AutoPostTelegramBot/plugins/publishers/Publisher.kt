package dev.inmo.AutoPostTelegramBot.plugins.publishers

import dev.inmo.AutoPostTelegramBot.base.plugins.Plugin

interface Publisher : Plugin {
    suspend fun publishPost(postId: Int)
}