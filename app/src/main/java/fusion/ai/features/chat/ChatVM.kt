package fusion.ai.features.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fusion.ai.R
import fusion.ai.billing.Plan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.datasource.cache.entity.ChatEntity
import fusion.ai.datasource.cache.entity.LibraryToolEntity
import fusion.ai.features.chat.datasource.network.repository.ChatRepository
import fusion.ai.features.chat.datasource.network.request.OutgoingExtras
import fusion.ai.features.chat.datasource.network.request.OutgoingMessage
import fusion.ai.util.ErrorEvent
import fusion.ai.util.combineFlows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatVM @Inject constructor(
    private val chatRepository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
    private val settingDs: SettingsDataStore
) : ViewModel() {

    private val prompt = MutableStateFlow<String?>(null)
    private val accountId = settingDs.getUserId.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )
    private val localMessages = MutableStateFlow<List<ChatEntity>>(listOf())
    private val selectedTool = MutableStateFlow<LibraryToolEntity?>(null)
    private val selectedToolExtrasId = MutableStateFlow<Int?>(null)
    private val maxPromptLength = MutableStateFlow(40)

    val isSendEnabled = chatRepository.isSendEnabled.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        true
    )

    private val errorEvent = MutableStateFlow<ErrorEvent?>(null)

    val apiKey = settingDs.getApiKey.distinctUntilChanged().filterNotNull()

    val state: StateFlow<ChatState> = combineFlows(
        combine(localMessages, chatRepository.getChats()) { a, b -> a + b },
        prompt,
        accountId,
        selectedTool,
        settingDs.getCurrentPlan.distinctUntilChanged(),
        maxPromptLength,
        errorEvent
    ) { response, prompt, accountId, selectedTool, userPlan, promptLength, errorEvent ->
        maxPromptLength.update {
            when (userPlan) {
                Plan.Trial -> 40
                Plan.Monthly -> 100
                Plan.Lifetime -> 2000
            }
        }
        ChatState(
            messages = response,
            prompt = prompt ?: "",
            isAuthenticated = accountId != null,
            selectedTool = selectedTool,
            maxPromptLength = promptLength,
            errorEvent = errorEvent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatState())

    init {
        val presetId: Int? = savedStateHandle["presetId"]
        val toolId: Int? = savedStateHandle["toolId"]
        selectedToolExtrasId.update { savedStateHandle.get<String?>("extrasId")?.toInt() }

        setTool(presetId = presetId, toolId = toolId)
    }

    private fun setTool(presetId: Int?, toolId: Int?) {
        viewModelScope.launch {
            chatRepository.getLibraryTool(
                presetId = presetId,
                toolId = toolId
            ).also { tool ->
                selectedTool.update { tool }
            }
        }
    }

    fun removeTool() {
        selectedTool.update { null }
        selectedToolExtrasId.update { null }
    }

    fun connectToChat(forceConnect: Boolean = false) {
        Timber.d("Re triggering")
        viewModelScope.launch {
            chatRepository.initiateSessionAndObserveMessage(forceConnect)
                .collectLatest { chatEntity ->
                    localMessages.update { chatEntity?.let { listOf(it) } ?: listOf() }
                }
        }
    }

    fun onMessageChange(message: String?) {
        if ((message?.length ?: 0) <= maxPromptLength.value) {
            prompt.update { message }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            chatRepository.resetSession()
        }
    }

    fun sendMessage() {
        val prompt = prompt.value
        val selectedToolId = selectedTool.value?.id
        val selectedToolExtrasId = selectedToolExtrasId.value

        viewModelScope.launch {
            val currentPlan = settingDs.getCurrentPlan.first()
            val apiKey = settingDs.getApiKey.first()
            if (currentPlan == Plan.Lifetime && apiKey == null) {
                errorEvent.emit(ErrorEvent(message = R.string.api_key_missing))
                return@launch
            }

            if (!prompt.isNullOrBlank()) {
                val request = if (selectedToolExtrasId != null) {
                    OutgoingMessage(
                        prompt,
                        selectedToolId,
                        extras = OutgoingExtras(id = selectedToolExtrasId)
                    )
                } else {
                    OutgoingMessage(prompt, selectedToolId)
                }
                chatRepository.sendMessage(request)
                onMessageChange(null)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
