package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class FinanceRate(
    @SerializedName("symbol") var symbol: FinanceSymbol? = null,
    @SerializedName("bid") val bid: Values?,
    @SerializedName("ask") val ask: Values?
)

data class Values(
    @SerializedName("rate") val rate: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("volume") val volume: Double,
    @SerializedName("count") val count: Int
)

enum class FinanceSymbol {
    @SerializedName("USD") USD,
    @SerializedName("EUR") EUR,
    @SerializedName("GBP") GBP;
}
