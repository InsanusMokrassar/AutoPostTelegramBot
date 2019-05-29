package com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.checkedMessagesFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.types.MessageEntity.BotCommandMessageEntity
import com.github.insanusmokrassar.TelegramBotAPI.types.UpdateIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.CommonMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.TextContent
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.UpdateReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

abstract class Command {
    val callback: UpdateReceiver<BaseMessageUpdate>
        get() = this::invoke
    protected abstract val commandRegex: Regex

    init {
        CoroutineScope(Dispatchers.Default).launch {
            checkedMessagesFlow.collectWithErrors {
                invoke(it)
            }
        }
    }

    suspend fun invoke(p1: BaseMessageUpdate) {
        (p1.data as? CommonMessage<*>) ?.let { message ->
            (message.content as? TextContent) ?.also {
                it.entities.firstOrNull {
                    it is BotCommandMessageEntity && (commandRegex.matches(it.command) || commandRegex.matches(it.sourceString))
                } ?.also {
                    onCommand(p1.updateId, message)
                } ?: if (commandRegex.matches(it.text)) {
                    onCommand(p1.updateId, message)
                }
            }
        }
    }

    abstract suspend fun onCommand(updateId: UpdateIdentifier, message: CommonMessage<*>)
}

fun buildCommandFlow(
    commandRegex: Regex
): Flow<CommonMessage<TextContent>> = checkedMessagesFlow.mapNotNull {
    val data = it.data
    if (data is CommonMessage<*>) {
        val contentAsText = data.content as? TextContent ?: return@mapNotNull null
        val contentEntities = contentAsText.entities
        contentEntities.firstOrNull { entity ->
            entity is BotCommandMessageEntity && commandRegex.matches(entity.command)
        } ?.let {
            return@mapNotNull data as CommonMessage<TextContent>
        }
    }
    null
}
