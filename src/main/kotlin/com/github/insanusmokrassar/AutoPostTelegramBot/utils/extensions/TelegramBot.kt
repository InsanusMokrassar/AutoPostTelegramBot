package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.github.insanusmokrassar.AutoPostTelegramBot.utils.SemaphoreK
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.ref.WeakReference

private val logger = LoggerFactory.getLogger("TelegramAsyncExecutions")

//private val semaphores: MutableMap<TelegramBot, SemaphoreK> = HashMap()
//
//fun initSemaphore(
//    executor: RequestsExecutor,
//    maxCount: Int = 30,
//    regenDelay: Long = 1000L,
//    regenCount: Int = 1
//) {
//    semaphores[bot] ?.also {
//        throw IllegalStateException("Semaphore for bot was initiated, but was initiated again")
//    } ?: bot.also {
//        semaphores[it] = SemaphoreK(maxCount).also {
//            launch {
//                while (isActive) {
//                    it.free(regenCount)
//                    delay(regenDelay)
//                }
//            }
//        }
//    }
//}
//
//private class DefaultCallback<T: BaseRequest<T, R>, R: BaseResponse>(
//        private val onFailureCallback: ((T, IOException?) -> Unit)?,
//        private val onResponseCallback: ((T, R) -> Unit)?,
//        executor: RequestsExecutor,
//        private var retries: Int = 0,
//        private val retriesDelay: Long = 1000L
//) : Callback<T, R> {
//    private val bot = WeakReference(bot)
//    override fun onFailure(request: T, e: IOException?) {
//        logger.warn("Request failure: {}; Error: {}", request, e)
//        onFailureCallback ?. invoke(request, e)
//        if (retries > 0) {
//            async {
//                delay(retriesDelay)
//                bot.get() ?. executeAsync(
//                    request,
//                    onFailureCallback,
//                    onResponseCallback,
//                    retries - 1,
//                    retriesDelay
//                )
//            }
//        }
//    }
//
//    override fun onResponse(request: T, response: R) {
//        logger.info("Request success: {}\nResponse: {}", request, response)
//        if (response.isOk) {
//            onResponseCallback ?. invoke(request, response)
//        } else {
//            onFailure(request, IOException(response.description()))
//        }
//    }
//}
//
//fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeAsync(
//        request: T,
//        onFailure: ((T, IOException?) -> Unit)? = null,
//        onResponse: ((T, R) -> Unit)? = null,
//        retries: Int = 0,
//        retriesDelay: Long = 1000L
//) {
//    if (onFailure == null && onResponse == null) {
//        launch {
//            executeBlocking(request, retries, retriesDelay)
//        }
//    } else {
//        launch {
//            try {
//                val result = executeBlocking(request, retries, retriesDelay)
//                onResponse ?.invoke(request, result)
//            } catch (e: IOException) {
//                onFailure ?.invoke(request, e)
//            }
//        }
//    }
//}
//
//@Throws(IOException::class)
//suspend fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeBlocking(
//        request: T,
//        retries: Int = 0,
//        retriesDelay: Long = 1000L
//): R {
//    semaphores[this@executeBlocking] ?.lock()
//    logger.info("Try to put request for executing: {}", request)
//    return suspendCoroutine {
//        continuation ->
//        execute(
//            request,
//            DefaultCallback(
//                {
//                    _, ioException ->
//                    continuation.resumeWithException(
//                        ioException ?: IllegalStateException("Something went wrong")
//                    )
//                },
//                {
//                    _, r ->
//                    continuation.resume(r)
//                },
//                this@executeBlocking,
//                retries,
//                retriesDelay
//            )
//        )
//    }
//}
//
//fun TelegramBot.queryAnswer(
//        id: String,
//        answerText: String,
//        asAlert: Boolean = false
//) {
//    executeAsync(
//            AnswerCallbackQuery(
//                    id
//            )
//                    .text(answerText)
//                    .showAlert(asAlert)
//    )
//}


