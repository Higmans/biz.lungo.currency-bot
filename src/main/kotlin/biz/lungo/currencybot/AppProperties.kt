package biz.lungo.currencybot

import io.ktor.client.plugins.logging.*

data class AppProperties(
    val appHost: String,
    val appPort: String,
    val mongoHost: String,
    val mongoPort: String,
    val mongoUser: String,
    val mongoPassword: String,
    val telegramApiToken: String,
    val cmcApiToken: String,
    val logLevel: LogLevel,
    val httpTimeoutMillis: Long
) {
    companion object {
        operator fun invoke() = with(PropertiesReader) {
            AppProperties(
                getProperty(key = APP_HOST_KEY),
                getProperty(APP_PORT_KEY),
                getProperty(MONGO_HOST_KEY),
                getProperty(MONGO_PORT_KEY),
                getProperty(MONGO_USER_KEY),
                getProperty(MONGO_PASSWORD_KEY),
                getProperty(TELEGRAM_API_TOKEN_KEY),
                getProperty(CMC_API_TOKEN_KEY),
                getProperty(LOG_LEVEL_KEY).toLogLevel(),
                try {
                    getProperty(HTTP_TIMEOUT_MILLIS_KEY).toLong()
                } catch (e: NumberFormatException) {
                    DEFAULT_TIMEOUT_MILLIS
                }
            )
        }
    }
}

private fun String.toLogLevel() = LogLevel.entries.firstOrNull { it.name == this } ?: LogLevel.NONE