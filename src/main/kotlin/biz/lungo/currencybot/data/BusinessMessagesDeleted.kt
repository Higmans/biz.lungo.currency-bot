package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class BusinessMessagesDeleted(

    @SerializedName("business_connection_id") var businessConnectionId: String,
    @SerializedName("chat") var chat: Chat,
    @SerializedName("date") var date: Long,
    @SerializedName("message_ids") var messageIds: List<Long>

)
