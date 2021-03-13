package dev.inmo.AutoPostTelegramBot.utils.CallbackQueryReceivers

import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.types.CallbackQuery.MessageDataCallbackQuery
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.update.CallbackQueryUpdate

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