package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class PhotoRequest(

    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("photo") val photoUrl: String,
    @SerializedName("business_connection_id") val businessConnectionId: String? = null

)
