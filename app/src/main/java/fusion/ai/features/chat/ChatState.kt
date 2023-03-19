package fusion.ai.features.chat

import fusion.ai.datasource.cache.entity.ChatEntity
import fusion.ai.datasource.cache.entity.LibraryToolEntity
import fusion.ai.util.ErrorEvent

data class ChatState(
    val messages: List<ChatEntity> = listOf(),
    val prompt: String = "",
    val isAuthenticated: Boolean = false,
    val selectedTool: LibraryToolEntity? = null,
    val maxPromptLength: Int = 40,
    val errorEvent: ErrorEvent? = null
)
