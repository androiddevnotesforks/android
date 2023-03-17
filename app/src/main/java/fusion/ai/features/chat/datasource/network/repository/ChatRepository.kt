package fusion.ai.features.chat.datasource.network.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import fusion.ai.billing.Plan
import fusion.ai.datasource.cache.Database
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.datasource.cache.entity.ChatEntity
import fusion.ai.datasource.cache.entity.Completion
import fusion.ai.datasource.cache.entity.LibraryPresetEntity
import fusion.ai.datasource.cache.entity.LibraryToolEntity
import fusion.ai.datasource.cache.entity.MessageEntity
import fusion.ai.datasource.cache.entity.MessageRole
import fusion.ai.datasource.cache.entity.MessageType
import fusion.ai.datasource.network.Endpoints
import fusion.ai.features.chat.datasource.network.dto.ChatResponseDto
import fusion.ai.features.chat.datasource.network.dto.LibraryPresetDto
import fusion.ai.features.chat.datasource.network.request.OutgoingMessage
import fusion.ai.util.isNullOrDefault
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class ChatRepository @Inject constructor(
    private val client: HttpClient,
    private val database: Database,
    private val gson: Gson,
    private val settingDs: SettingsDataStore
) {
    private var socket: WebSocketSession? = null
    private val chatDao = database.chatDao()
    private val libraryPresetsDao = database.libraryPresetsDao()
    private var signInMessageSent = false
    private var errorMessageEmitted = false
    private val _isSendEnabled = MutableStateFlow(true)
    val isSendEnabled = _isSendEnabled.asStateFlow()

    suspend fun getLibraryTool(presetId: Int?, toolId: Int?): LibraryToolEntity? {
        if (presetId.isNullOrDefault() || toolId.isNullOrDefault()) {
            return null
        }
        requireNotNull(presetId)
        return libraryPresetsDao.getPresetById(presetId)
            ?.tools?.find { tool -> tool.id == toolId }
    }

    fun getChats() = chatDao.getChats()

    fun initiateSessionAndObserveMessage() = channelFlow {
        when (val userId = settingDs.getUserId.first()) {
            null -> {
                sendLocalMessage(
                    message = "\uD83D\uDC4B Hey, Please sign in to start using HandyAI.",
                    MessageType.SignInRequest
                )
                return@channelFlow
            }

            else -> {
                send(null)
                val connection = initSession(userId).getOrThrow()
                if (connection) {
                    observeMessages()
                        .launchIn(this)
                } else {
                    Timber.d("Connection failed")
                }
            }
        }
    }.catch {
        if (!errorMessageEmitted) {
            errorMessageEmitted = true
            emit(
                ChatEntity(
                    type = MessageType.Error,
                    message = null,
                    chatId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun initSession(id: String): Result<Boolean> {
        Timber.d("User id $id")
        val attachApiKey = settingDs.getCurrentPlan.first() == Plan.Lifetime
        val apiKey = if (attachApiKey) settingDs.getApiKey.first() else null
        Timber.d("Initiating connection $attachApiKey $apiKey")
        return try {
            socket = client.webSocketSession {
                url(Endpoints.ChatSocket.build())
                parameter("id", id)
                parameter("api", apiKey)
            }
            if (socket?.isActive == true) {
                Result.success(true)
            } else {
                Timber.d("connection failed")
                Result.failure(Exception("Failed to establish connection!"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendMessage(outgoingMessage: OutgoingMessage) {
        try {
            val message = Gson().toJson(outgoingMessage)
            socket?.send(Frame.Text(message))
            _isSendEnabled.update { false }
//            insertLocalChat(
//                ChatEntity(
//                    type = MessageType.TextGeneration,
//                    message = MessageEntity(
//                        content = outgoingMessage.content,
//                        role = MessageRole.User,
//                    ),
//                    chatId = UUID.randomUUID().toString(),
//                    timestamp = System.currentTimeMillis()
//                )
//            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun ProducerScope<ChatEntity>.sendLocalMessage(
        message: String,
        type: MessageType
    ) {
        if (type == MessageType.SignInRequest && !signInMessageSent) {
            send(
                ChatEntity(
                    type = type,
                    message = MessageEntity(
                        message,
                        role = MessageRole.Assistant
                    ),
                    chatId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
            signInMessageSent = true
        }
    }

    private fun observeMessages(): Flow<ChatEntity?> {
        var chatId = "-1"
        val chatMutex = Mutex()
        return socket?.incoming
            ?.receiveAsFlow()
            ?.filter { it is Frame.Text }
            ?.map {
                val json = (it as? Frame.Text)?.readText() ?: throw UnknownError()
                gson.fromJson(json, ChatResponseDto::class.java)
            }
            ?.mapNotNull { messageDto ->
                when (Completion.valueOf(messageDto.completion)) {
                    Completion.Progress -> {
                        val type = MessageType.valueOf(messageDto.type)
                        _isSendEnabled.update {
                            type != MessageType.TextGeneration && type != MessageType.ImageGeneration
                        }
                        when (type) {
                            MessageType.Welcome -> {
                                val entity = chatDao.getChatById(id = WELCOME_MESSAGE_ID)
                                // we don't want to flood users with welcome message!
                                if (entity != null) {
                                    chatDao.updateChat(entity.copy(timestamp = System.currentTimeMillis()))
                                } else {
                                    chatDao.insertChat(messageDto.toChatEntity())
                                }
                                return@mapNotNull entity
                            }
                            MessageType.ApiKeyMissing -> {
                                val entity = chatDao.getChatById(id = API_KEY_MISSING_ID)
                                // we don't want to flood users with this message!
                                if (entity != null) {
                                    chatDao.updateChat(entity.copy(timestamp = System.currentTimeMillis()))
                                } else {
                                    chatDao.insertChat(messageDto.toChatEntity())
                                }
                                return@mapNotNull entity
                            }
                            else -> database.withTransaction {
                                chatMutex.withLock {
                                    if (messageDto.id != chatId) {
                                        chatId = messageDto.id
                                        chatDao.insertChat(messageDto.toChatEntity())
                                    } else {
                                        val getChat =
                                            chatDao.getChatById(messageDto.id)
                                                ?: throw Exception()
                                        chatDao.updateChat(
                                            getChat.copy(
                                                message = messageDto.message?.toMessageEntity(),
                                                type = try {
                                                    MessageType.valueOf(messageDto.type)
                                                } catch (e: Exception) {
                                                    Timber.e(e)
                                                    getChat.type
                                                },
                                                timestamp = messageDto.timeStamp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    /** Completion [Completion.ImageCompletion] & [Completion.ImageCompletion] */
                    else -> {
                        _isSendEnabled.update { true }
                    }
                }
                chatDao.getChatById(chatId)
            }
            ?.flowOn(Dispatchers.IO) ?: flowOf()
    }

    suspend fun pauseSession() =
        socket?.close(CloseReason(CloseReason.Codes.NORMAL, PAUSE_SESSION_MESSAGE))

    suspend fun resetSession() =
        socket?.close(CloseReason(CloseReason.Codes.NORMAL, RESET_SESSION_MESSAGE))

    fun getFlowPresets(): Flow<Result<List<LibraryPresetEntity>>> = flow {
        val response = try {
            withContext(Dispatchers.IO) {
                client.get(Endpoints.ChatTools.build()).body<List<LibraryPresetDto>>().map {
                    LibraryPresetEntity(
                        id = it.id,
                        title = it.title,
                        tools = it.tools.map { tool ->
                            tool.toEntity()
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            val dataFromCache = libraryPresetsDao.getPresets().takeIf {
                it.isNotEmpty()
            } ?: kotlin.run {
                emit(Result.failure(e))
                return@flow
            }
            emit(Result.success(dataFromCache))
            return@flow
        }

        database.withTransaction {
            with(libraryPresetsDao) {
                deleteAll()
                insertPresets(response)
            }
        }
        emit(Result.success(libraryPresetsDao.getPresets()))
    }

    companion object {
        const val WELCOME_MESSAGE_ID = "Welcome"
        const val API_KEY_MISSING_ID = "ApiKeyMissing"
        const val PAUSE_SESSION_MESSAGE = "Paused"
        const val RESET_SESSION_MESSAGE = "Reset"
    }
}
