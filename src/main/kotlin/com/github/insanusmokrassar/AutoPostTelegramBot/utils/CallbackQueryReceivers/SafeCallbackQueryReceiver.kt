package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.github.insanusmokrassar.AutoPostTelegramBot.callbackQueryListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.NewDefaultCoroutineScope
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.types.CallbackQuery.*
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.Update

private val SafeCallbackQueryReceiversScope = NewDefaultCoroutineScope()

abstract class SafeCallbackQueryReceiver(
    executor: RequestsExecutor,
    private val checkChatId: ChatIdentifier
) : CallbackQueryReceiver(
    executor
) {
    init {
        callbackQueryListener.subscribe(
            scope = SafeCallbackQueryReceiversScope
        ) { update ->
        }
    }

    override suspend fun invoke(update: Update<CallbackQuery>) {
        val query = update.data as? MessageDataCallbackQuery ?: return
        if (query.message.chat.id == checkChatId) {
            invoke(query)
        }
    }

    abstract suspend fun invoke(query: MessageDataCallbackQuery)
}