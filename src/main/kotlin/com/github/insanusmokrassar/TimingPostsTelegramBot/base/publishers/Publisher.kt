package com.github.insanusmokrassar.TimingPostsTelegramBot.base.publishers

interface Publisher {
    fun publishPost(postId: Int)
}