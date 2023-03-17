package fusion.ai.datasource.cache.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat")
data class ChatEntity(
    val type: MessageType,
    @Embedded
    val message: MessageEntity?,
    val completion: Completion = Completion.Progress,
    val timestamp: Long,
    val chatId: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)

enum class MessageType {
    Welcome, TextGeneration, ImageGeneration, Error, LowChatToken, LowImageToken, SignInRequest, ApiKeyMissing;
}

enum class MessageRole {
    User, Assistant
}

enum class Completion {
    TextCompletion, ImageCompletion, Progress
}

@Entity(tableName = "chat_message")
data class MessageEntity(
    val content: String,
    val role: MessageRole,
    @PrimaryKey(autoGenerate = true)
    val messageId: Int = 0
) {
    val contentIsImage get() = content.startsWith("https://") && content.endsWith(".jpeg")
}
