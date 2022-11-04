package biz.lungo.currencybot

import biz.lungo.currencybot.plugins.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

lateinit var telegramClient: HttpClient
lateinit var nbuClient: HttpClient
lateinit var cmcClient: HttpClient
lateinit var telegramApiToken: String
lateinit var botPath: String
lateinit var cmcApiToken: String

val pinnedMessagesFile: File
    get() =
        File("pinnedIds.csv").apply {
            if (!exists()) {
                createNewFile()
                csvWriter().open(this) {
                    writeRow("chatId", "messageId")
                }
            }
        }

val lastUpdatedFile = File("last-updated")

fun main(args: Array<String>) {

    args.get("-t")?.let {
        telegramApiToken = it
        botPath = it.split(":")[1]
    } ?: run {
        println("Please specify Telegram Bot token with -t arg")
        return
    }

    args.get("-c")?.let {
        cmcApiToken = it
    } ?: run {
        println("Please specify CoinMarketCap token with -c arg")
        return
    }

    telegramClient = HttpClient(Apache) {
        engine { followRedirects = false }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    nbuClient = HttpClient(Apache) {
        engine { followRedirects = false }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    cmcClient = HttpClient(Apache) {
        engine { followRedirects = false }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    embeddedServer(Netty, port = 8082, host = "127.0.0.1") {
        configureRouting()
        configureSerialization()
        configureMonitoring()
        configureBot()
    }.start()

    if (!lastUpdatedFile.exists() || currentPrices.isEmpty()) {
        runBlocking {
            launch {
                updateRates(lastUpdatedFile)
                fetchNbuRates()
                startPinnedMessagePolling()
            }
        }
    }
//    startMinfinScraping()
}