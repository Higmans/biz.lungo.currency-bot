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
    var jokeText = ""

    val collection = configDb.getCollection<LastKnownJoke>()
    val lastKnownJoke = collection.find().first()?.value
    if (lastKnownJoke == null) {
        collection.insertOne(LastKnownJoke(DEFAULT_LAST_KNOWN_JOKE))
    }

    val jokeId = Random.nextInt(1, lastKnownJoke ?: DEFAULT_LAST_KNOWN_JOKE)
    val jokeUrl = if (jokeId <= OLD_FORMAT_MAX_JOKE_ID) "$RANDOM_JOKE_URL$jokeId" else "$NEW_JOKE_URL$jokeId/"
    println("Fetching joke from: $jokeUrl")

    try {
        skrape(HttpFetcher) {
            request { url = jokeUrl }
            response {
                htmlDocument {
                    jokeText = findFirst(".coupon-field") {
                        text
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Error fetching joke from $jokeUrl: ${e.message}")
    }

    if (jokeText.isBlank()) {
        println("Warning: joke text is empty for URL $jokeUrl")
    }
    return jokeText
}

suspend fun updateLastKnownJoke() {
    println("Updating last known joke from: $LAST_KNOWN_JOKE_URL")
    val count: Int? = try {
        skrape(HttpFetcher) {
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
                        println("Parsed text from index page: $allText")

                        val countFromText = Regex("""(\d+)\s+анекдотів""")
                            .findAll(allText)
                            .lastOrNull()
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.toIntOrNull()
                        println("Extracted joke count: $countFromText")
                        if (countFromText != null) return@div countFromText else null
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Error updating last known joke: ${e.message}")
        null
    }
    if (count != null) {
        val collection = configDb.getCollection<LastKnownJoke>()
        collection.drop()
        collection.insertOne(LastKnownJoke(count))
        println("Updated last known joke to: $count")
    } else {
        println("Warning: could not determine last known joke count")
    }
}

suspend fun getMemeUrl(): String = randomMemeClient.get(RANDOM_MEME_URL) {
    contentType(ContentType.Text.Plain)
}.body<String>()
