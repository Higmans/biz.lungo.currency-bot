package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class ChatType {
    @SerializedName("group") GROUP,
    @SerializedName("private") PRIVATE,
    @SerializedName("supergroup") SUPERGROUP,
    @SerializedName("channel") CHANNEL
}