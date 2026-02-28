package biz.lungo.currencybot.plugins

import biz.lungo.currencybot.*
import biz.lungo.currencybot.data.*
import biz.lungo.currencybot.data.FinanceSymbol.*
import biz.lungo.currencybot.plugins.Cryptocurrency.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

private val br = System.lineSeparator()
private val waitingForReply = CopyOnWriteArrayList<Long>()
private val businessConnectionCanReply = ConcurrentHashMap<String, Boolean>()

private val botInfoCollection = configDb.getCollection<BotUser>()
private val pinnedDb = mongoClient.getDatabase("pinned")
private val pinnedCollection = pinnedDb.getCollection<PinnedMessageInfo>()

fun Application.configureBot() {

    runBlocking {
        launch {
            delay(15.seconds)
            fetchNbuRates()
            fetchFinanceRates()
            botInfoCollection.drop()
            val botUser = getMe()
            botInfoCollection.insertOne(BotUser(botUser.id, botUser.username, botUser.firstName))
            if (!PropertiesReader.isDebug()) refreshPinnedMessages(this@configureBot.log)
        }
    }

    routing {

        post("/$botPath") {
            val update = call.receive<MessageResponse>()
            call.respondText("OK")
            val businessConnection = update.businessConnection
            if (businessConnection != null) {
                this@configureBot.log.info("Business connection event: ${businessConnection.id}, canReply=${businessConnection.canReply}")
                businessConnectionCanReply[businessConnection.id] = businessConnection.canReply
                return@post
            }
            val message = update.message ?: update.businessMessage ?: return@post
            val messageText = message.text
            val chatId = message.chat.id
            val businessConnectionId = message.businessConnectionId
            if (businessConnectionId != null && businessConnectionCanReply[businessConnectionId] == false) return@post
            val diff = ChronoUnit.MINUTES.between(
                Date(message.date * 1000).toInstant().atZone(gmtPlus3),
                Instant.now().atZone(gmtPlus3)
            )
            if (diff > 10) return@post

            if (waitingForReply.contains(chatId)) {
                if (messageText.isNullOrBlank()) return@post
                waitingForReply.remove(chatId)
                val output = formatNbuRatesResponse(messageText, getNbuRates())
                sendTelegramMessage(chatId, output, businessConnectionId = businessConnectionId)
                return@post
            }
            when(messageText.parseCommand()) {
                Command.Start -> {
                    if (businessConnectionId != null) {
                        sendTelegramMessage(chatId, "Привіт! \uD83D\uDC4B Я готовий до роботи в бізнес-чаті.", businessConnectionId = businessConnectionId)
                    } else {
                        val savedPinnedMessage = getPinnedMessagesInfo().find { it.chatId == chatId }
                        if (savedPinnedMessage != null) {
                            sendTelegramMessage(chatId, "Я вже стартував в цьому чаті, спробуй інші команди \uD83D\uDE44")
                        } else {
                            sendTelegramMessage(chatId, "Привіт! \uD83D\uDC4B Оновлюю курси...")
                            fetchNbuRates()
                            delay(3.seconds)
                            sendAndPinMessage(chatId)
                        }
                    }
                }
                Command.NbuRate -> {
                    val botUser = botInfoCollection.find().first() ?: return@post
                    val typingJob = BotTypingJob(chatId, businessConnectionId).start(this)
                    val regex = Pattern.compile("${Command.NbuRate.commandText}(@${botUser.username})?")
                    val param = messageText?.split(regex)?.getOrNull(1)?.trim()
                    if (param?.isNotBlank() == true) {
                        sendTelegramMessage(chatId, formatNbuRatesResponse(param, getNbuRates()), businessConnectionId = businessConnectionId)
                    } else {
                        sendTelegramMessage(chatId, "Яка саме валюта цікавить?", businessConnectionId = businessConnectionId)
                        waitingForReply.add(chatId)
                    }
                    typingJob.finish()
                }
                Command.Crypto -> {
                    val typingJob = BotTypingJob(chatId, businessConnectionId).start(this)
                    val rates = getCryptoRates(listOf(BTC, ETH, XRP, DOGE, DOT, CAKE, TON, TRUMP))
                    sendTelegramMessage(chatId, "${rates.data.btc.symbol}: $${rates.data.btc.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.eth.symbol}: $${rates.data.eth.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.xrp.symbol}: $${rates.data.xrp.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.doge.symbol}: $${rates.data.doge.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.dot.symbol}: $${rates.data.dot.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.cake.symbol}: $${rates.data.cake.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.ton.symbol}: $${rates.data.ton.quote.quoteValue.price.formatValue()}${br}" +
                            "${rates.data.trump.symbol}: $${rates.data.trump.quote.quoteValue.price.formatValue()}", businessConnectionId = businessConnectionId)
                    typingJob.finish()
                }
                Command.Joke -> {
                    val typingJob = BotTypingJob(chatId, businessConnectionId).start(this)
                    sendTelegramMessage(chatId, getNewJoke(), businessConnectionId = businessConnectionId)
                    typingJob.finish()
                }
                Command.Oil -> {
                    val typingJob = BotTypingJob(chatId, businessConnectionId).start(this)
                    val oilPrices = getOilPrices()
                    sendTelegramMessage(chatId, "Ціни на нафту:${br}Brent: $${oilPrices.brent.formatValue()}${br}WTI: $${oilPrices.wti.formatValue()}", businessConnectionId = businessConnectionId)
                    typingJob.finish()
                }
                Command.Meme -> {
                    sendTelegramPhoto(chatId, getMemeUrl(), businessConnectionId = businessConnectionId)
                }
                else -> Unit
            }
        }

        post("/refreshNbu") {
            val secret = call.receive<RefreshRequest>().secret
            if (secret == botPath) {
                call.respondText("OK")
                fetchNbuRates()
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid secret")
            }
        }

        post("/refreshFinance") {
            val secret = call.receive<RefreshRequest>().secret
            if (secret == botPath) {
                call.respondText("OK")
                fetchFinanceRates()
                refreshPinnedMessages(this@configureBot.log)
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid secret")
            }
        }

        post("/refreshJokes") {
            val secret = call.receive<RefreshRequest>().secret
            if (secret == botPath) {
                call.respondText("OK")
                updateLastKnownJoke()
            } else {
                call.respond(HttpStatusCode.Forbidden, "Invalid secret")
            }
        }
    }
}

private fun formatNbuRatesResponse(input: String?, nbuRates: List<NbuRate>): String {
    if (input?.equals("хуй", true) == true) {
        return "Хуя ти вумний \uD83C\uDF46\uD83C\uDF46\uD83C\uDF46"
    }
    val foundNbuRates = ArrayList(nbuRates.filter { input?.contains(it.symbol, true) == true || it.name.contains(input ?: "~*(#&~!", true) })
    val splitMessage = input?.split("[, ]".toRegex())
    if (splitMessage?.isNotEmpty() == true) {
        foundNbuRates.clear()
        splitMessage.filter { it.isNotBlank() }.forEach { splitString ->
            foundNbuRates.addAll(nbuRates.filter { splitString.contains(it.symbol, true) || it.name.contains(splitString, true) })
        }
    }
    val finalList = foundNbuRates.distinctBy { it.symbol }
    when (finalList.size) {
        0 -> return "Не знайшов такої валюти \uD83E\uDD37"
        1 -> return finalList.first().let { "Курс НБУ для ${it.symbol} (${it.name}): ${it.rate.formatValue()}" }
        else -> {
            val responseBuilder = StringBuilder()
            finalList.forEachIndexed { index, nbuRate ->
                if (index > 0) {
                    responseBuilder.append(System.lineSeparator())
                } else {
                    responseBuilder.append("Знайдені валюти (курс НБУ):")
                    responseBuilder.append(System.lineSeparator())
                }
                responseBuilder.append("${nbuRate.symbol} (${nbuRate.name}): ${nbuRate.rate.formatValue()}")
            }
            return responseBuilder.toString()
        }
    }
}

suspend fun refreshPinnedMessages(logger: Logger) {
    val usd = getFinanceRate(USD)
    val eur = getFinanceRate(EUR)
    val gbp = getFinanceRate(GBP)
    val btc = getCryptoRates(listOf(BTC)).data.btc
    if (usd == null || eur == null || gbp == null) return
    getPinnedMessagesInfo().forEach { messageInfo ->
        try {
            editMessage(messageInfo.chatId, messageInfo.messageId, getFormattedPinnedMessage(usd, eur, gbp, btc))
        } catch (e: ClientRequestException) {
            val message = e.localizedMessage
            logger.error("Error: $message")
            if (message.contains("bot was kicked from the group chat") || message.contains("message to edit not found")) {
                removeChatId(messageInfo.chatId)
            }
        }
    }
}

private suspend fun removeChatId(chatId: Long) {
    pinnedCollection.deleteOne("{chatId: $chatId}")
}

private suspend fun getPinnedMessagesInfo(): List<PinnedMessageInfo> = pinnedCollection.find().toList()

private fun getFormattedPinnedMessage(usd: FinanceRate, eur: FinanceRate, gbp: FinanceRate, btc: Coin): String {
    return "\uD83D\uDCC8${br}$: ${usd.formatBidAsk()}${br}€: ${eur.formatBidAsk()}${br}£: ${gbp.formatBidAsk()}${br}฿: $${btc.quote.quoteValue.price.formatValue()}"
}

private fun FinanceRate.formatBidAsk() = if (bid?.rate == null || ask?.rate == null) {
    "~/~"
} else {
    "${bid.rate.formatValue()}/${ask.rate.formatValue()}${getChangeSymbol()}"
}

private fun FinanceRate.getChangeSymbol() = when {
    (this.ask?.change ?: 0.0) > 0 -> "↑"
    (this.ask?.change ?: 0.0) < 0 -> "↓"
    else -> "="
}

private suspend fun sendAndPinMessage(chatId: Long) {
    val usd = getFinanceRate(USD)
    val eur = getFinanceRate(EUR)
    val gbp = getFinanceRate(GBP)
    val btc = getCryptoRates(listOf(BTC)).data.btc
    if (usd == null || eur == null || gbp == null) {
        sendTelegramMessage(chatId, "Не вдалося оновити курси валют \uD83D\uDE1E")
        return
    }
    val messageIdToPin = sendTelegramMessage(chatId, getFormattedPinnedMessage(usd, eur, gbp, btc)).result.messageId
    try {
        pinMessage(chatId, messageIdToPin)
        pinnedCollection.insertOne(PinnedMessageInfo(chatId, messageIdToPin))
    } catch (e: Exception) {
        println("Error: ${e.localizedMessage}")
    }
}

private suspend fun sendTelegramMessage(chatId: Long, message: String, markdown: Boolean = false, businessConnectionId: String? = null) =
    telegramClient.post("$BOT_API_URL/bot$telegramApiToken/sendMessage") {
        contentType(ContentType.Application.Json)
        setBody(MessageRequest(chatId = chatId, text = message, parseMode = if (markdown) ParseMode.HTML.value else null, businessConnectionId = businessConnectionId))
    }.body<SendMessageResult>()

private suspend fun sendTelegramPhoto(chatId: Long, photoUrl: String, businessConnectionId: String? = null) =
    telegramClient.post("$BOT_API_URL/bot$telegramApiToken/sendPhoto") {
        contentType(ContentType.Application.Json)
        setBody(PhotoRequest(chatId = chatId, photoUrl = photoUrl, businessConnectionId = businessConnectionId))
    }.body<SendMessageResult>()

private suspend fun pinMessage(chatId: Long, messageId: Long) {
    telegramClient.post("$BOT_API_URL/bot$telegramApiToken/pinChatMessage") {
        contentType(ContentType.Application.Json)
        setBody(PinMessageRequest(chatId, messageId, true))
    }.body<HttpResponse>()
}

private suspend fun editMessage(chatId: Long, messageId: Long, text: String, businessConnectionId: String? = null) {
    telegramClient.post("$BOT_API_URL/bot$telegramApiToken/editMessageText") {
        contentType(ContentType.Application.Json)
        setBody(EditMessageRequest(chatId, messageId, text, businessConnectionId))
    }.body<HttpResponse>()
}

private suspend fun getCryptoRates(currencies: List<Cryptocurrency>) =
    cmcClient.get("${CRYPTO_RATES_BASE_URL}${currencies.formatParam()}") {
        contentType(ContentType.Application.Json)
        headers {
            append("X-CMC_PRO_API_KEY", cmcApiToken)
        }
    }.body<CmcResponse>()

private suspend fun getMe() = telegramClient.post("$BOT_API_URL/bot$telegramApiToken/getMe") {
    contentType(ContentType.Application.Json)
}.body<GetMeResult>().result

private fun String?.parseCommand() = Command.entries.find { this != null && this.startsWith(it.commandText) }

private fun Double.formatValue() = String.format(locale = Locale.US, "%.${if (this < 1) "3" else "2"}f", this)

private fun List<Cryptocurrency>.formatParam() = this.joinToString(separator = ",") { it.name.lowercase(Locale.getDefault()) }

private enum class Command(val commandText: String) {
    Start("/start"),
    NbuRate("/nburate"),
    Crypto("/crypto"),
    Joke("/joke"),
    Oil("/oil"),
    Meme("/meme")
}

private data class PinnedMessageInfo(
    val chatId: Long,
    val messageId: Long
)

private enum class Cryptocurrency {
    BTC, ETH, XRP, DOGE, DOT, CAKE, TON, TRUMP
}