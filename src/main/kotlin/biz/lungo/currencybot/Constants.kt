package biz.lungo.currencybot

const val BOT_API_URL = "https://api.telegram.org"
const val NBU_RATES_PATH_FORMAT = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?date=%s&json"
const val FINANCE_RATES_PATH_FORMAT = "https://labc.finance.ua/dailycurrencycash/jsonsrc/statisticsfortable/%s"
const val RANDOM_JOKE_URL = "https://rozdil.lviv.ua/anekdot/anekdot.php?id="
const val LAST_KNOWN_JOKE_URL = "https://rozdil.lviv.ua/anekdot/anekdot.php"
const val CRYPTO_RATES_BASE_URL = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?symbol="
const val OIL_URL = "https://deeptest.lungo.biz/oil.html"
const val RANDOM_MEME_URL = "http://memes:8087/random"
const val DEFAULT_TIMEOUT_MILLIS = 150000L
const val DEFAULT_LAST_KNOWN_JOKE = 17785

const val APP_HOST_KEY = "APP_HOST"
const val APP_PORT_KEY = "APP_PORT"
const val MONGO_HOST_KEY = "MONGO_HOST"
const val MONGO_PORT_KEY = "MONGO_PORT"
const val MONGO_USER_KEY = "MONGO_USER"
const val MONGO_PASSWORD_KEY = "MONGO_PASSWORD"
const val TELEGRAM_API_TOKEN_KEY = "TELEGRAM_API_TOKEN"
const val CMC_API_TOKEN_KEY = "CMC_API_TOKEN"
const val LOG_LEVEL_KEY = "LOG_LEVEL"
const val HTTP_TIMEOUT_MILLIS_KEY="HTTP_TIMEOUT_MILLIS"