package biz.lungo.currencybot

import biz.lungo.currencybot.data.LastKnownJoke
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.*
import kotlin.random.Random

suspend fun getNewJoke(): String {
    var text = ""

    val collection = configDb.getCollection<LastKnownJoke>()
    val lastKnownJoke = collection.find().first()?.value
    if (lastKnownJoke == null) {
        collection.insertOne(LastKnownJoke(DEFAULT_LAST_KNOWN_JOKE))
    }

    skrape(HttpFetcher) {
        request { url = RANDOM_JOKE_URL + Random.nextInt(1, lastKnownJoke ?: DEFAULT_LAST_KNOWN_JOKE) }
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