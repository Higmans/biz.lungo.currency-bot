package biz.lungo.currencybot.plugins

import biz.lungo.currencybot.*
import biz.lungo.currencybot.data.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

var pinnedMessageId: Long = -1

@OptIn(ExperimentalTime::class)
fun Application.configureBot() {

    routing {

        post("/$botPath") {
            call.respondText("OK")
            val message = call.receive<MessageResponse>().message
            val replyToMessage = message?.replyToMessage
            val messageText = message?.text
            val chatId = message?.chat?.id ?: return@post
            val diff = ChronoUnit.MINUTES.between(Date(message.date * 1000).toInstant().atZone(ZoneId.systemDefault()), Instant.now().atZone(ZoneId.systemDefault()))
            if (diff > 10) return@post
            when(messageText.parseCommand()) {
                Command.Start -> {
                    sendTelegramMessage(chatId, "Привіт! \uD83D\uDC4B Оновлюю курси...")
                    updateRates()
                    delay(10.seconds)
                    startUpdatingPinnedMessage(chatId)
                    delay(1.hours)
                    startPinnedMessagePolling()
                }
                Command.Rates -> {
                    val responseBuilder = StringBuilder()
                    currentPrices.forEach { currency ->
                        responseBuilder.append(System.lineSeparator())
                        responseBuilder.append("<b>${currency.currency}</b>:${System.lineSeparator()}міжбанк - ${currency.bank.buy.formatValue()}/${currency.bank.sell.formatValue()}${System.lineSeparator()}НБУ - ${currency.nbu.formatValue()}${System.lineSeparator()}ринок - ${currency.market.buy.formatValue()}/${currency.market.sell.formatValue()}${System.lineSeparator()}")
                    }
                    sendTelegramMessage(chatId, responseBuilder.toString(), markdown = true)
                }
                Command.Update -> {
                    sendTelegramMessage(chatId, "Оновлюю курси...")
                    updateRates()
                    delay(10.seconds)
                    sendAndPinMessage(chatId)
                }
                Command.NbuRate -> {
                    sendTelegramMessage(chatId, "Яка саме валюта цікавить?", ReplyMarkup(true, "USD, GBP або JPY"))
                }
                Command.Crypto -> {
                    val rates = getCryptoRates()
                    sendTelegramMessage(chatId, "${rates.data.btc.symbol}: $${rates.data.btc.quote.quoteValue.price.formatValue()}${System.lineSeparator()}${rates.data.eth.symbol}: $${rates.data.eth.quote.quoteValue.price.formatValue()}${System.lineSeparator()}${rates.data.xrp.symbol}: $${rates.data.xrp.quote.quoteValue.price.formatValue()}")
                }
            }

            if (replyToMessage?.from?.isBot == true) {
                if (messageText?.equals("хуй", true) == true) {
                    sendTelegramMessage(chatId, "Хуя ти вумний \uD83C\uDF46\uD83C\uDF46\uD83C\uDF46")
                    return@post
                }
                val foundNbuRates = ArrayList(nbuRates.filter { messageText?.contains(it.symbol, true) == true || it.name.contains(messageText ?: "~*(#&~!", true) })
                val splitMessage = messageText?.split("[, ]".toRegex())
                if (splitMessage?.isNotEmpty() == true) {
                    foundNbuRates.clear()
                    splitMessage.filter { it.isNotBlank() }.forEach { splitString ->
                        foundNbuRates.addAll(nbuRates.filter { splitString.contains(it.symbol, true) || it.name.contains(splitString, true) })
                    }
                }
                val finalList = foundNbuRates.distinctBy { it.symbol }
                when (finalList.size) {
                    1 -> {
                        val nbuRate = finalList.first()
                        sendTelegramMessage(chatId, "Курс НБУ для ${nbuRate.symbol} (${nbuRate.name}): ${nbuRate.rate.formatValue()}")
                    }
                    0 -> {
                        sendTelegramMessage(chatId, "Не знайшов такої валюти \uD83E\uDD37")
                    }
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
                        sendTelegramMessage(chatId, responseBuilder.toString())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun startPinnedMessagePolling() {
    while (true) {
        startUpdatingPinnedMessage()
        delay(1.hours)
    }
}

private suspend fun sendAndPinMessage(chatId: Long) {
    val usd = currentPrices.find { it.currency == "USD" }
    val eur = currentPrices.find { it.currency == "EUR" }
    val gbp = currentPrices.find { it.currency == "GBP" }
    val formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM")
    if (pinnedMessageId > 0) {
        editMessage(chatId, pinnedMessageId, formatPinnedMessage(
            usd,
            eur,
            gbp,
            LocalDateTime.now().format(formatter)
        ))
    } else {
        val messageIdToPin = sendTelegramMessage(chatId, formatPinnedMessage(
            usd,
            eur,
            gbp,
            LocalDateTime.now().format(formatter)
        )).result.messageId
        pinnedMessageId = messageIdToPin
        pinMessage(chatId, messageIdToPin)
    }
}

private suspend fun startUpdatingPinnedMessage(chatId: Long? = null, start: Boolean = false) {
    chatId?.toString()?.let { chatIdString ->
        csvReader().openAsync(pinnedMessagesFile) {
            readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                if (row.containsKey(chatIdString)) {
                    val savedMessageId = row[chatIdString] ?: "-1"
                    pinnedMessageId = if (start) {
                        unpinMessage(chatIdString.toLong(), savedMessageId.toLong())
                        -1
                    } else {
                        savedMessageId.toLong()
                    }
                }
            }
        }
    }
    if (pinnedMessagesFile.exists()) {
        val text = pinnedMessagesFile.readText()
        val split = text.split(":")
        val savedChatId = split[0]
        val savedMessageId = split[1]
        pinnedMessageId = if (start) {
            unpinMessage(savedChatId.toLong(), savedMessageId.toLong())
            -1
        } else {
            savedMessageId.toLong()
        }
        chatId?.let { sendAndPinMessage(it) }
    } else {
        chatId?.let {
            sendAndPinMessage(it)
            pinnedMessagesFile.writeText("$it:$pinnedMessageId")
        }
    }
}

private fun formatPinnedMessage(
    usd: CurrencyModel?,
    eur: CurrencyModel?,
    gbp: CurrencyModel?,
    timestamp: String
) =
    "$: ${usd?.market?.buy?.formatValue()}/${usd?.market?.sell?.formatValue()}, €: ${eur?.market?.buy?.formatValue()}/${eur?.market?.sell?.formatValue()}, £: ${gbp?.market?.buy?.formatValue()}/${gbp?.market?.sell?.formatValue()} ($timestamp)"

private suspend fun sendTelegramMessage(chatId: Long, message: String, replyMarkup: ReplyMarkup? = null, markdown: Boolean = false) =
    telegramClient.post<SendMessageResult>("$BOT_API_URL/bot$telegramApiToken/sendMessage") {
        contentType(ContentType.Application.Json)
        body = MessageRequest(chatId = chatId, text = message, if (markdown) ParseMode.HTML.value else null, replyMarkup)
    }

private suspend fun pinMessage(chatId: Long, messageId: Long) {
    telegramClient.post<HttpResponse>("$BOT_API_URL/bot$telegramApiToken/pinChatMessage") {
        contentType(ContentType.Application.Json)
        body = PinMessageRequest(chatId, messageId, true)
    }
}

private suspend fun unpinMessage(chatId: Long, messageId: Long) {
    telegramClient.post<HttpResponse>("$BOT_API_URL/bot$telegramApiToken/unpinChatMessage") {
        contentType(ContentType.Application.Json)
        body = PinMessageRequest(chatId, messageId, true)
    }
}

private suspend fun editMessage(chatId: Long, messageId: Long, text: String) {
    telegramClient.post<HttpResponse>("$BOT_API_URL/bot$telegramApiToken/editMessageText") {
        contentType(ContentType.Application.Json)
        body = EditMessageRequest(chatId, messageId, text)
    }
}

private suspend fun getCryptoRates() =
    cmcClient.get<CmcResponse>("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?symbol=btc,eth,xrp") {
        contentType(ContentType.Application.Json)
        headers {
            append("X-CMC_PRO_API_KEY", cmcApiToken)
        }
    }

private fun String?.parseCommand() = Command.values().find { this != null && this.contains(it.commandText) }

private fun Double.formatValue() = String.format(locale = Locale.US, "%.${if (this < 1) "3" else "2"}f", this)

enum class Command(val commandText: String) {
    Start("/start"),
    Rates("/rates"),
    Update("/update"),
    NbuRate("/nburate"),
    Crypto("/crypto")
}