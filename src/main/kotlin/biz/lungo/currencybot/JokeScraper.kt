package biz.lungo.currencybot

import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.*

suspend fun getNewJoke(): String {
    var text = ""

    skrape(HttpFetcher) {
        request { url = RANDOM_JOKE_URL }
        response {
            htmlDocument {
                body {
                    table {
                        withClass = "text"
                        text = findFirst {
                            tbody {
                                tr {
                                    td {
                                        findFirst {
                                            this.text
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return text
}