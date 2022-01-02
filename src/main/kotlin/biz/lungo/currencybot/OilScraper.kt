package biz.lungo.currencybot

import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.td

suspend fun getOilPrices() =
    skrape(BrowserFetcher) {
        request { url = OIL_URL }
        return@skrape response {
            return@response htmlDocument {
                return@htmlDocument td {
                    findAll {
                        OilPrices(this[1].text.toDouble(), this[4].text.toDouble())
                    }
                }
            }
        }
    }

data class OilPrices(
    val brent: Double,
    val wti: Double
)