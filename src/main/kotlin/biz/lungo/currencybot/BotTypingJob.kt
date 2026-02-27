package biz.lungo.currencybot

import biz.lungo.currencybot.data.ActionType
import biz.lungo.currencybot.data.ChatAction
import biz.lungo.currencybot.data.ChatActionResult
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*

class BotTypingJob(private val chatId: Long, private val businessConnectionId: String? = null) {

    private var job: Job? = null

    fun start(scope: CoroutineScope): BotTypingJob {
        val progressSteps = (appProperties.httpTimeoutMillis / TG_TYPING_SINGLE_DURATION_MILLIS).toInt()
        job = scope.launch {
            withTimeout(appProperties.httpTimeoutMillis) {
                withContext(Dispatchers.IO) {
                    repeat(progressSteps) { _ ->
                        sendChatTypingAction()
                        delay(TG_TYPING_SINGLE_DURATION_MILLIS)
                    }
                }
            }
        }
        return this
    }

    fun finish() {
        job?.cancel()
    }

    private suspend fun sendChatTypingAction() =
        telegramClient.post("$BOT_API_URL/bot$telegramApiToken/sendChatAction") {
            contentType(ContentType.Application.Json)
            setBody(ChatAction(chatId, ActionType.TYPING, businessConnectionId))
        }.body<ChatActionResult>()

    companion object {
        // according to doc https://core.telegram.org/bots/api#sendchataction
        private const val TG_TYPING_SINGLE_DURATION_MILLIS = 5000L
    }
}