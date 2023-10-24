package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class CmcResponse(
    @SerializedName("status") val status: Status,
    @SerializedName("data") val data: Data
)

data class Status(
    @SerializedName("error_code") val errorCode: Int,
    @SerializedName("error_message") val errorMessage: String?
)

data class Data(
    @SerializedName("BTC") val btc: Coin,
    @SerializedName("ETH") val eth: Coin,
    @SerializedName("XRP") val xrp: Coin,
    @SerializedName("DOGE") val doge: Coin,
    @SerializedName("DOT") val dot: Coin,
    @SerializedName("CAKE") val cake: Coin
)

data class Coin(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("quote") val quote: Quote
)

data class Quote(
    @SerializedName("USD") val quoteValue: QuoteValue
)

data class QuoteValue(
    @SerializedName("price") val price: Double
)