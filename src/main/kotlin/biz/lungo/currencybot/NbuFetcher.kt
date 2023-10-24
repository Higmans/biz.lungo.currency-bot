package biz.lungo.currencybot

import biz.lungo.currencybot.data.NbuRate
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.time.Instant
import java.time.format.DateTimeFormatter

private val ratesDb = mongoClient.getDatabase("rates")
private val nbuCollection = ratesDb.getCollection<NbuRate>()

suspend fun fetchNbuRates() {
    nbuCollection.drop()
    val nbuRates = arrayListOf<NbuRate>()
    nbuRates.addAll(
        nbuClient.get(NBU_RATES_PATH_FORMAT.format(getFormattedDate())) {
            contentType(ContentType.Application.Json)
        }.body<List<NbuRate>>()
    )
    nbuRates.add(NbuRate("UAH", "Українська Гривня", 1.0))
    nbuCollection.insertMany(nbuRates)
}

private fun getFormattedDate() =
    Instant.now().atZone(gmtPlus3).format(DateTimeFormatter.ofPattern("YYYYMMdd"))

suspend fun getNbuRates(): List<NbuRate> {
    return nbuCollection.find().toList()
}