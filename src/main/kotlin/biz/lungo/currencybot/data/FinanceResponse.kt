package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class FinanceResponse(
    @SerializedName("date") val date: String,
    @SerializedName("data") val data: FinanceRate
)