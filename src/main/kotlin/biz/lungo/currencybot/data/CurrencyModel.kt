package biz.lungo.currencybot.data

data class CurrencyModel(
    val currency: String,
    val bank: Price,
    val nbu: Double,
    val market: Price
)

data class Price(
    val buy: Double,
    val sell: Double
)
