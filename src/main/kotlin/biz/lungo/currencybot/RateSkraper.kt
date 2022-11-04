package biz.lungo.currencybot

import biz.lungo.currencybot.data.CurrencyModel
import biz.lungo.currencybot.data.Price
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.tr
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

private val UPDATE_THRESHOLD = 1.hours.inWholeMilliseconds
private val REFRESH_BACKOFF = 3.minutes.inWholeMilliseconds
private val generalRegex = "(?<buy>\\d+,\\d+).*/.* (?<sell>\\d+,\\d+)".toRegex()
private val nbuRegex = "(?<buy>\\d+\\.\\d+)".toRegex()
private var lastRefreshed = 0L
val currentPrices = arrayListOf<CurrencyModel>()

@OptIn(ExperimentalTime::class)
fun startMinfinScraping() {
    Timer().schedule(0, 1.hours.inWholeMilliseconds) {
        runBlocking {
            launch {
                if (!lastUpdatedFile.exists() || currentPrices.isEmpty() || shouldUpdate(lastUpdatedFile)) {
                    updateRates(lastUpdatedFile)
                }
            }
        }
    }
}

suspend fun updateRates(lastUpdatedFile: File? = null) {
    if (!shouldRefresh()) return
    lastRefreshed = now()
    skrape(HttpFetcher) {
        request { url = "https://minfin.com.ua/currency/kiev/" }
        response {
            htmlDocument {
                tr {
                    findAll {
                        val rows = arrayListOf<CurrencyModel>()
                        forEach {
                            val currency = it.children.find { doc -> doc.className == "mfcur-table-cur" }?.text ?: ""
                            val bank =
                                it.children.find { doc -> doc.attribute("data-title") == "Средний курс в банках" }?.text
                                    ?: ""
                            val nbu = it.children.find { child -> child.attribute("data-title") == "НБУ" }?.text ?: ""
                            val market =
                                it.children.find { child -> child.attribute("data-title") == "Черный рынок" }?.text
                                    ?: ""
                            if (currency.isNotEmpty()) {
                                rows.add(CurrencyModel(currency, bank.toPrice(), nbu.toNbuPrice(), market.toPrice()))
                            }
                        }
                        lastUpdatedFile?.writeText(now().toString())
                        currentPrices.clear()
                        currentPrices.addAll(rows)
                    }
                }
            }
        }
    }
}

private fun now() = System.currentTimeMillis()

private fun shouldUpdate(lastUpdatedFile: File) =
    now() - lastUpdatedFile.readText().toLong() > UPDATE_THRESHOLD

private fun shouldRefresh() =
    now() - lastRefreshed > REFRESH_BACKOFF

private fun String.toPrice(): Price {
    val priceMatch = generalRegex.find(this)
    val rawBuyPrice = (priceMatch?.let { it.groups["buy"]?.value } ?: "0").replace(",", ".").toDouble()
    val rawSellPrice = (priceMatch?.let { it.groups["sell"]?.value } ?: "0").replace(",", ".").toDouble()
    return Price(rawBuyPrice, rawSellPrice)
}

private fun String.toNbuPrice(): Double {
    val priceMatch = nbuRegex.find(this)
    return (priceMatch?.let { it.groups["buy"]?.value } ?: "0").replace(",", ".").toDouble()
}