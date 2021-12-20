package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class PinMessageRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("disable_notification") val disableNotification: Boolean
)
