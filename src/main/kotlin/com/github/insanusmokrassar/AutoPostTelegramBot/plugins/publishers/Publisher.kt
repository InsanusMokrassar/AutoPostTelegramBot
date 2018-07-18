package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin

interface Publisher : Plugin {
    fun publishPost(postId: Int)
}