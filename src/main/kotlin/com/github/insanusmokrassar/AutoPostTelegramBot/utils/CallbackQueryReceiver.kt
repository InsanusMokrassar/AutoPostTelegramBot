package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers.UnsafeCallbackQueryReceiver
import com.pengrad.telegrambot.TelegramBot

@Deprecated(
    "Will be removed for the reason of extending of callback query receivers",
    ReplaceWith("UnsafeCallbackQueryReceiver")
)
abstract class CallbackQueryReceiver(
    bot: TelegramBot
) : UnsafeCallbackQueryReceiver(bot)