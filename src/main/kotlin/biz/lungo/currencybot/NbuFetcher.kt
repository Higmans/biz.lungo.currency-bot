package biz.lungo.currencybot

import biz.lungo.currencybot.data.NbuRate
import io.ktor.client.request.*
import io.ktor.http.*

val nbuRates = arrayListOf<NbuRate>()

suspend fun fetchNbuRates() {
    nbuRates.clear()
    nbuRates.addAll(
        nbuClient.get<List<NbuRate>>(NBU_RATES_PATH) {
            contentType(ContentType.Application.Json)
        }
    )
    nbuRates.add(NbuRate("UAH", "Українська Гривня", 1.0))
}