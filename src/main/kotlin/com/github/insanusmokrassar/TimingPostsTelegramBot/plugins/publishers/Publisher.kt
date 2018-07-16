package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.publishers

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.Plugin

interface Publisher : Plugin {
    fun publishPost(postId: Int)
}