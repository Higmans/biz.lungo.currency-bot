package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class ChatActionResult(

    @SerializedName("ok") var ok: Boolean,
    @SerializedName("result") var result: Boolean

)
