package biz.lungo.currencybot

import biz.lungo.currencybot.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {

    cmcApiToken = appProperties.cmcApiToken

    nbuClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        engine { followRedirects = false }
        install(ContentNegotiation) {
            gson()
        }
    }

    financeClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        engine { followRedirects = false }
        install(ContentNegotiation) {
            gson()
        }
    }

    cmcClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        engine { followRedirects = false }
        install(ContentNegotiation) {
            gson()
        }
    }

    randomMemeClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
        engine { followRedirects = false }
    }

    runBlocking {
        launch {
            val joke = getNewJoke()
            println("ASDK: $joke")
        }
    }

    embeddedServer(Netty, port = appProperties.appPort.toInt(), host = appProperties.appHost) {
        configureRouting()
        configureSerialization()
        configureMonitoring()
    }.start(true)
}