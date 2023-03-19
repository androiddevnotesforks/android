package fusion.ai.features.chat.datasource.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import fusion.ai.datasource.cache.entity.ChatEntity
import fusion.ai.datasource.cache.entity.Completion
import fusion.ai.datasource.cache.entity.MessageEntity
import fusion.ai.datasource.cache.entity.MessageRole
import fusion.ai.datasource.cache.entity.MessageType

@Keep
data class ChatResponseDto(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("type")
    val type: String,
    @SerializedName("completion")
    val completion: String,
    @SerializedName("message")
    val message: MessageDto?,
    @SerializedName("availableToken")
    val availableToken: Int,
    @SerializedName("availableImageToken")
    val availableImageToken: Int,
    @SerializedName("timeStamp")
    val timeStamp: Long,
    @SerializedName("id")
    val id: String
) {
    fun toChatEntity(): ChatEntity {
        return ChatEntity(
            type = MessageType.valueOf(type),
            message = message?.toMessageEntity(),
            chatId = id,
            timestamp = System.currentTimeMillis(),
            completion = Completion.valueOf(completion)
        )
    }
}

@Keep
data class MessageDto(
    @SerializedName("content")
    val content: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("name")
    val name: String? = null
) {
    fun toMessageEntity(): MessageEntity {
        return MessageEntity(
            content = content,
            role = MessageRole.valueOf(role.replaceFirstChar { it.uppercase() })
        )
    }
}
