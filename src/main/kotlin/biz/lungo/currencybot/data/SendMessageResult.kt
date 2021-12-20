package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class SendMessageResult(

    @SerializedName("ok") var ok: Boolean,
    @SerializedName("result") var result: Result

)

data class Result(

    @SerializedName("message_id") var messageId: Long,
    @SerializedName("from") var from: From,
    @SerializedName("chat") var chat: Chat,
    @SerializedName("date") var date: Long,
    @SerializedName("text") var text: String

)
