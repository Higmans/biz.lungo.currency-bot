package biz.lungo.currencybot

import biz.lungo.currencybot.plugins.*
import com.mongodb.ConnectionString
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.time.ZoneId
import java.time.ZoneOffset

lateinit var telegramClient: HttpClient
lateinit var nbuClient: HttpClient
lateinit var financeClient: HttpClient
lateinit var cmcClient: HttpClient
lateinit var mongoClient: CoroutineClient
lateinit var cmcApiToken: String
lateinit var configDb: CoroutineDatabase

val appProperties = AppProperties()
val telegramApiToken = appProperties.telegramApiToken
val botPath = telegramApiToken.split(":")[1]
val gmtPlus3: ZoneId = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(3))

private val mongoConnection = with(appProperties) { ConnectionString("mongodb://$mongoUser:$mongoPassword@$mongoHost:$mongoPort") }

fun main() {

    cmcApiToken = appProperties.cmcApiToken

    telegramClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        engine { followRedirects = false }
        install(ContentNegotiation) {
            gson()
        }
    }

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

    mongoClient = KMongo.createClient(mongoConnection).coroutine
    configDb = mongoClient.getDatabase("config")

    runBlocking {
        launch {
            fetchNbuRates()
            fetchFinanceRates()
            if (!PropertiesReader.isDebug()) {
                refreshPinnedMessages()
            }
        }
    }

    embeddedServer(Netty, port = appProperties.appPort.toInt(), host = appProperties.appHost) {
        configureRouting()
        configureSerialization()
        configureMonitoring()
        configureBot()
    }.start(true)
}