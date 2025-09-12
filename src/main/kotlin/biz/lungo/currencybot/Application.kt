package biz.lungo.currencybot

import biz.lungo.currencybot.plugins.*
import com.mongodb.ConnectionString
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.net.SocketException
import java.time.ZoneId
import java.time.ZoneOffset

lateinit var telegramClient: HttpClient
lateinit var nbuClient: HttpClient
lateinit var financeClient: HttpClient
lateinit var cmcClient: HttpClient
lateinit var mongoClient: CoroutineClient
lateinit var randomMemeClient: HttpClient
lateinit var cmcApiToken: String
lateinit var configDb: CoroutineDatabase

val appProperties = AppProperties()
val telegramApiToken = appProperties.telegramApiToken
val     botPath = telegramApiToken.split(":")[1]
val gmtPlus3: ZoneId = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(3))

private val mongoConnection = with(appProperties) { ConnectionString("mongodb://$mongoUser:$mongoPassword@$mongoHost:$mongoPort") }

fun main() {

    cmcApiToken = appProperties.cmcApiToken

    // Configure HTTP client for Telegram API communication
    telegramClient = HttpClient(Apache) {
        // Enable logging for debugging
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
        // Disable automatic redirect following
        engine { followRedirects = false }
        // Add JSON content negotiation
        install(ContentNegotiation) {
            gson()
        }
        // Configure retry behavior for failed requests
        install(HttpRequestRetry) {
            maxRetries = 5
            // Retry on non-success responses except 400 Bad Request
            retryIf { _, response ->
                !response.status.isSuccess() && response.status.value != HttpStatusCode.BadRequest.value
            }
            // Retry on network socket exceptions
            retryOnExceptionIf { _, cause ->
                cause is SocketException
            }
            // Exponential backoff delay between retries
            delayMillis { retry ->
                val baseDelay = 1000L
                val maxDelay = 20000L // Cap at 20 seconds
                val exponentialDelay = minOf(baseDelay * (1 shl retry), maxDelay)
                // Add random jitter of ±20%
                (exponentialDelay * (0.8 + Math.random() * 0.4)).toLong()
            }
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

    randomMemeClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
        engine { followRedirects = false }
    }

    mongoClient = KMongo.createClient(mongoConnection).coroutine
    configDb = mongoClient.getDatabase("config")

    embeddedServer(Netty, port = appProperties.appPort.toInt(), host = appProperties.appHost) {
        configureRouting()
        configureSerialization()
        configureMonitoring()
        configureBot()
    }.start(true)
}