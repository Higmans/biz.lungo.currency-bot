package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class RefreshRequest(

    @SerializedName("secret") val secret: String?

)