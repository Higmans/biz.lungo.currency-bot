package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class EditMessageRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("text") val text: String
)