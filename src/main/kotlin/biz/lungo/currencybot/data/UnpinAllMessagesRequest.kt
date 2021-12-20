package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class UnpinAllMessagesRequest(
    @SerializedName("chat_id") val chatId: Long
)
