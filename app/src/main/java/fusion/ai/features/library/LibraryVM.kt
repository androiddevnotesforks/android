package fusion.ai.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fusion.ai.R
import fusion.ai.billing.Plan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.datasource.cache.entity.LibraryPresetEntity
import fusion.ai.features.chat.datasource.network.repository.ChatRepository
import fusion.ai.util.ErrorEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryVM @Inject constructor(
    private val chatRepository: ChatRepository,
    settingDs: SettingsDataStore
) : ViewModel() {
    private val isLoading = MutableStateFlow(false)

    private val presets = MutableStateFlow<List<LibraryPresetEntity>>(listOf())

    private val errorEvent = MutableStateFlow<ErrorEvent?>(null)

    val state: StateFlow<LibraryState> = combine(
        isLoading,
        presets,
        errorEvent,
        settingDs.getCurrentPlan
    ) { isLoading, presets, errorEvent, currentPlan ->
        LibraryState(
            isLoading = isLoading,
            presets = presets,
            errorEvent = errorEvent,
            userPlan = currentPlan
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    init {
        fetchPresets()
    }

    fun refresh() {
        fetchPresets()
    }

    private fun fetchPresets() {
        viewModelScope.launch {
            chatRepository.getFlowPresets()
                .onStart { isLoading.update { true } }
                .onCompletion { isLoading.update { false } }
                .collectLatest { result ->
                    when {
                        result.isSuccess -> {
                            presets.update { result.getOrDefault(listOf()) }
                        }
                        result.isFailure -> {
                            errorEvent.emit(ErrorEvent(message = R.string.generic_error_message))
                        }
                    }
                }
        }
    }
}

data class LibraryState(
    val isLoading: Boolean = true,
    val presets: List<LibraryPresetEntity> = listOf(),
    val errorEvent: ErrorEvent? = null,
    val userPlan: Plan? = null
)
