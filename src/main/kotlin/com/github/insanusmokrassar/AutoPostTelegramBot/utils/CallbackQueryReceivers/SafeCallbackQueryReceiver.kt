package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.MessageDataCallbackQuery
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.update.CallbackQueryUpdate

abstract class SafeCallbackQueryReceiver(
    executor: RequestsExecutor,
    private val checkChatId: ChatIdentifier
) : CallbackQueryReceiver(
    executor
) {
    override suspend fun invoke(update: CallbackQueryUpdate) {
        val query = update.data as? MessageDataCallbackQuery ?: return
        if (query.message.chat.id == checkChatId) {
            invoke(query)
        }
    }

    abstract suspend fun invoke(query: MessageDataCallbackQuery)
}