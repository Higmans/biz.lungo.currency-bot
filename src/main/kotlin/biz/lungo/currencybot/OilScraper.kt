package biz.lungo.currencybot

import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.td

suspend fun getOilPrices() =
    OilPrices(fetchOilPrice(BRENT_OIL_URL), fetchOilPrice(WTI_OIL_URL))

private suspend fun fetchOilPrice(siteUrl: String) =
    skrape(BrowserFetcher) {
        request { url = siteUrl }
        return@skrape response {
            return@response htmlDocument {
                return@htmlDocument td {
                    findAll {
                        this[1].text.toDouble()
                    }
                }
            }
        }
    }

data class OilPrices(
    val brent: Double,
    val wti: Double
)