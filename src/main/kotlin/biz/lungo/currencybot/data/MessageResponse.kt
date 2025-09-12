package biz.lungo.currencybot.data

import com.google.gson.annotations.SerializedName

data class MessageResponse(

    @SerializedName("update_id") var updateId: Long,
    @SerializedName("message") var message: Message?

)

data class Chat(

    @SerializedName("id") var id: Long,
    @SerializedName("first_name") var firstName: String? = null,
    @SerializedName("username") var username: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("type") var type: ChatType

)

data class From(

    @SerializedName("id") var id: Long? = null,
    @SerializedName("is_bot") var isBot: Boolean? = null,
    @SerializedName("first_name") var firstName: String? = null,
    @SerializedName("username") var username: String? = null,
    @SerializedName("language_code") var languageCode: String? = null

)

data class Message(

    @SerializedName("message_id") var messageId: Long,
    @SerializedName("from") var from: From,
    @SerializedName("chat") var chat: Chat,
    @SerializedName("date") var date: Long,
    @SerializedName("text") var text: String,
    @SerializedName("reply_to_message") var replyToMessage: Message? = null,
    @SerializedName("author_signature") var authorSignature: String? = null,
    @SerializedName("sender_boost_count") var senderBoostCount: Int? = null,
    @SerializedName("reply_markup") var replyMarkup: ReplyMarkup? = null,
    @SerializedName("has_protected_content") var hasProtectedContent: Boolean? = null,
    @SerializedName("is_topic_message") var isTopicMessage: Boolean? = null,
    @SerializedName("message_thread_id") var messageThreadId: Int? = null

)