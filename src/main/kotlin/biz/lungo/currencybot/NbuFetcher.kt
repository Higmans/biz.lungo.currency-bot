package biz.lungo.currencybot

import biz.lungo.currencybot.data.NbuRate
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.hours

val nbuRates = arrayListOf<NbuRate>()

fun fetchNbuRates() {
    Timer().schedule(0, 3.hours.inWholeMilliseconds) {
        runBlocking {
            launch {
                nbuRates.clear()
                nbuRates.addAll(
                    nbuClient.get<List<NbuRate>>(NBU_RATES_PATH) {
                        contentType(ContentType.Application.Json)
                    }
                )
                nbuRates.add(NbuRate("UAH", "Українська Гривня", 1.0))
            }
        }
    }
}