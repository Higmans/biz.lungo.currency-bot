package biz.lungo.currencybot

import biz.lungo.currencybot.data.LastKnownJoke
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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
                        this.text
                    }
                }
            }
        }
    }
    return text
}

suspend fun updateLastKnownJoke() {
    val count: Int? = skrape(HttpFetcher) {
        request { url = LAST_KNOWN_JOKE_URL }
        response {
            htmlDocument {
                div {
                    withClass = "blockhoveranekdot"
                    val allText = findFirst {
                        div {
                            withClass = "textanpid"
                            findFirst {
                                text
                            }
                        }
                    }

                    val countFromText = Regex("""(\d+)\s+анекдотів""")
                        .findAll(allText)
                        .lastOrNull()
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                    if (countFromText != null) return@div countFromText else null
                }
            }
        }
    }
    if (count != null) {
        val collection = configDb.getCollection<LastKnownJoke>()
        collection.drop()
        collection.insertOne(LastKnownJoke(count))
    }
}

suspend fun getMemeUrl(): String = randomMemeClient.get(RANDOM_MEME_URL) {
    contentType(ContentType.Text.Plain)
}.body<String>()
