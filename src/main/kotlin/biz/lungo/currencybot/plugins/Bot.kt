package biz.lungo.currencybot.plugins

import biz.lungo.currencybot.*
import biz.lungo.currencybot.data.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
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
                    val savedPinnedMessage = getPinnedMessagesInfo().find { it.chatId == chatId }
                    if (savedPinnedMessage != null) {
                        sendTelegramMessage(chatId, "Я вже стартував в цьому чаті, спробуй інші команди \uD83D\uDE44")
                    } else {
                        sendTelegramMessage(chatId, "Привіт! \uD83D\uDC4B Оновлюю курси...")
                        lastUpdatedFile.delete()
                        delay(10.seconds)
                        sendAndPinMessage(chatId)
                    }
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
                    lastUpdatedFile.delete()
                    delay(10.seconds)
                    val savedPinnedMessage = getPinnedMessagesInfo().find { it.chatId == chatId }
                    if (savedPinnedMessage != null) {
                        editMessage(chatId, savedPinnedMessage.messageId, getFormattedPinnedMessage())
                    }
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
        getPinnedMessagesInfo().forEach { messageInfo -> editMessage(messageInfo.chatId, messageInfo.messageId, getFormattedPinnedMessage()) }
        delay(1.hours)
    }
}

private suspend fun getPinnedMessagesInfo(): List<PinnedMessageInfo> {
    val result = arrayListOf<PinnedMessageInfo>()
    csvReader().openAsync(pinnedMessagesFile) {
        val rows = readAllWithHeaderAsSequence()
        result.addAll(rows.map { it.toPinnedMessageInfo() })
    }
    return result
}

private fun getFormattedPinnedMessage(): String {
    val usd = currentPrices.find { it.currency == "USD" }
    val eur = currentPrices.find { it.currency == "EUR" }
    val gbp = currentPrices.find { it.currency == "GBP" }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM")
    return formatPinnedMessage(
        usd,
        eur,
        gbp,
        LocalDateTime.now().format(formatter)
    )
}

private suspend fun sendAndPinMessage(chatId: Long) {
    val messageIdToPin = sendTelegramMessage(chatId, getFormattedPinnedMessage()).result.messageId
    pinMessage(chatId, messageIdToPin)
    csvWriter().writeAll(listOf(listOf(chatId, messageIdToPin)), pinnedMessagesFile, true)
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

private fun Map<String, String>.toPinnedMessageInfo(): PinnedMessageInfo {
    val chatId = this["chatId"]?.toLong() ?: -1
    val messageId = this["messageId"]?.toLong() ?: -1
    return PinnedMessageInfo(chatId, messageId)
}

enum class Command(val commandText: String) {
    Start("/start"),
    Rates("/rates"),
    Update("/update"),
    NbuRate("/nburate"),
    Crypto("/crypto")
}

data class PinnedMessageInfo(
    val chatId: Long,
    val messageId: Long
)