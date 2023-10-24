package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class GetMeResult(

    @SerializedName("ok") var ok: Boolean,
    @SerializedName("result") var result: User

)
