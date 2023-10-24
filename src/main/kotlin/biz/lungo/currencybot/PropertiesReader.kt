package biz.lungo.currencybot

import kotlin.system.exitProcess

object PropertiesReader {

    fun getProperty(key: String): String {
        return System.getenv(key) ?: run {
            println("ERROR: $key property is missing in environmental variables.")
            exitProcess(404)
        }
    }

    fun isDebug() = System.getenv("CURRBOT_DEBUG")?.toBoolean() ?: false
}