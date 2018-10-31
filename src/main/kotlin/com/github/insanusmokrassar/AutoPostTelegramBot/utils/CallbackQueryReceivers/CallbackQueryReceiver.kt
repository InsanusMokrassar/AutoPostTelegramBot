package com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceivers

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import java.lang.ref.WeakReference

interface CallbackQueryReceiver : (CallbackQuery, TelegramBot) -> Unit
