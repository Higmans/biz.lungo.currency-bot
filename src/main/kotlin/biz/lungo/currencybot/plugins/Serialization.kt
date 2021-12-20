package biz.lungo.currencybot.plugins

import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.application.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            }
        json()
    }
}
