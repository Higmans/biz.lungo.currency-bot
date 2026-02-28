package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class BusinessMessagesDeleted(
    @SerializedName("business_connection_id") val businessConnectionId: String,
    @SerializedName("chat") val chat: Chat,
    @SerializedName("date") val date: Long,
    @SerializedName("message_ids") val messageIds: List<Long>
)
