package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class NbuRate(
    @SerializedName("cc") val symbol: String,
    @SerializedName("txt") val name: String,
    @SerializedName("rate") val rate: Double
)
