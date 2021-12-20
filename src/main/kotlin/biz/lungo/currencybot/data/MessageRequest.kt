package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class MessageRequest(

    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("text") val text: String,
    @SerializedName("parse_mode") val parseMode: String?,
    @SerializedName("reply_markup") val replyMarkup: ReplyMarkup? = null

)

data class ReplyMarkup(

    @SerializedName("force_reply") val forceReply: Boolean,
    @SerializedName("input_field_placeholder") val inputPlaceholder: String

)

enum class ParseMode(val value: String) {
    MARKDOWN_V2("MarkdownV2"),
    HTML("HTML")
}
