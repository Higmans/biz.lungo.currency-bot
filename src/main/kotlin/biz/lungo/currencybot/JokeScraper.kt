package biz.lungo.currencybot

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.*
import kotlin.random.Random

suspend fun getNewJoke(): String {
    var text = ""

    skrape(HttpFetcher) {
        request { url = RANDOM_JOKE_URL + Random.nextInt(1, 9247) }
        response {
            htmlDocument {
                p {
                    withClass = "coupon-field"
                    text = findFirst {
                        i {
                            findFirst {
                                this.text
                            }
                        }
                    }
                }
            }
        }
    }
    return text
}