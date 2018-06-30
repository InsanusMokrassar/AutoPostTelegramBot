package com.github.insanusmokrassar.TimingPostsTelegramBot.publishers

interface Publisher {
    fun publishPost(postId: Int)
}