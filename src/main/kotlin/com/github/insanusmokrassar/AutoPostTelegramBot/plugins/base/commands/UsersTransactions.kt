package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier

val usersTransactions = HashMap<ChatIdentifier, PostTransaction>()
