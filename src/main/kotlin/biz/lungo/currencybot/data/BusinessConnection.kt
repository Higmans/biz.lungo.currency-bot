package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class BusinessConnection(
    @SerializedName("id") val id: String,
    @SerializedName("user") val user: User,
    @SerializedName("user_chat_id") val userChatId: Long,
    @SerializedName("date") val date: Long,
    @SerializedName("can_reply") val canReply: Boolean,
    @SerializedName("is_enabled") val isEnabled: Boolean
)
