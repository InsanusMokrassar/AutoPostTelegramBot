package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.SemaphoreK
import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.experimental.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.suspendCoroutine

private val logger = LoggerFactory.getLogger("TelegramAsyncExecutions")

private val semaphores: MutableMap<TelegramBot, SemaphoreK> = HashMap()

fun initSemaphore(
    bot: TelegramBot,
    maxCount: Int = 30,
    regenDelay: Long = 1000L,
    regenCount: Int = 1
) {
    semaphores[bot] ?.also {
        throw IllegalStateException("Semaphore for bot was initiated, but was initiated again")
    } ?: bot.also {
        semaphores[it] = SemaphoreK(maxCount).also {
            launch {
                while (isActive) {
                    it.free(regenCount)
                    delay(regenDelay)
                }
            }
        }
    }
}

private class DefaultCallback<T: BaseRequest<T, R>, R: BaseResponse>(
        private val onFailureCallback: ((T, IOException?) -> Unit)?,
        private val onResponseCallback: ((T, R) -> Unit)?,
        bot: TelegramBot,
        private var retries: Int = 0,
        private val retriesDelay: Long = 1000L
) : Callback<T, R> {
    private val bot = WeakReference(bot)
    override fun onFailure(request: T, e: IOException?) {
        logger.warn("Request failure: {}; Error: {}", request, e)
        onFailureCallback ?. invoke(request, e)
        if (retries > 0) {
            async {
                delay(retriesDelay)
                bot.get() ?. executeAsync(
                    request,
                    onFailureCallback,
                    onResponseCallback,
                    retries - 1,
                    retriesDelay
                )
            }
        }
    }

    override fun onResponse(request: T, response: R) {
        logger.info("Request success: {}\nResponse: {}", request, response)
        if (response.isOk) {
            onResponseCallback ?. invoke(request, response)
        } else {
            onFailure(request, IOException(response.description()))
        }
    }
}

fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeAsync(
        request: T,
        onFailure: ((T, IOException?) -> Unit)? = null,
        onResponse: ((T, R) -> Unit)? = null,
        retries: Int = 0,
        retriesDelay: Long = 1000L
) {
    launch {
        if (onFailure == null && onResponse == null) {
            executeDeferred(request, retries, retriesDelay)
        } else {
            try {
                val result = executeDeferred(request, retries, retriesDelay).await()
                onResponse ?.invoke(request, result)
            } catch (e: IOException) {
                onFailure ?.invoke(request, e)
            }
        }
    }
}

@Throws(IOException::class)
suspend fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeDeferred(
        request: T,
        retries: Int = 0,
        retriesDelay: Long = 1000L
): Deferred<R> {
    return async {
        semaphores[this@executeDeferred] ?.lock()
        logger.info("Try to put request for executing: {}", request)
        suspendCoroutine<R> {
            continuation ->
            execute(
                request,
                DefaultCallback(
                    {
                        _, ioException ->
                        continuation.resumeWithException(
                            ioException ?: IllegalStateException("Something went wrong")
                        )
                    },
                    {
                        _, r ->
                        continuation.resume(r)
                    },
                    this@executeDeferred,
                    retries,
                    retriesDelay
                )
            )
        }
    }
}


@Throws(IOException::class)
fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeSync(
    request: T,
    retries: Int = 0,
    retriesDelay: Long = 1000L
): R {
    return runBlocking {
        this@executeSync.executeDeferred(request, retries, retriesDelay).await()
    }
}



fun TelegramBot.queryAnswer(
        id: String,
        answerText: String,
        asAlert: Boolean = false
) {
    executeAsync(
            AnswerCallbackQuery(
                    id
            )
                    .text(answerText)
                    .showAlert(asAlert)
    )
}


