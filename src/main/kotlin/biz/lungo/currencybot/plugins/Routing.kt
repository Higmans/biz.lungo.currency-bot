package biz.lungo.currencybot.plugins

import biz.lungo.currencybot.getNewJoke
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello! I am Lungo")
        }
        get("/joke") {
            call.respondText(getNewJoke())
        }
    }
}
