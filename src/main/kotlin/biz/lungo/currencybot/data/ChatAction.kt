package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class ChatAction(
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("action")
    val action: ActionType
)

@Suppress("unused")
enum class ActionType {
    @SerializedName("typing")
    TYPING,
    @SerializedName("upload_photo")
    UPLOAD_PHOTO,
    @SerializedName("choose_sticker")
    CHOOSE_STICKER
}