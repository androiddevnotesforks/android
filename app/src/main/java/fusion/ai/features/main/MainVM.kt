package fusion.ai.features.main

import android.content.Intent
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.features.signin.GoogleSignInHandler
import fusion.ai.features.signin.datasource.network.worker.SignInManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainVM @Inject constructor(
    private val signInHandler: GoogleSignInHandler,
    private val billingRepository: BillingRepository,
    private val settingDs: SettingsDataStore,
    private val signInManager: SignInManager
) : ViewModel() {

    private val signInState = MutableStateFlow<SignInState>(SignInState.Unknown)

    val state: StateFlow<MainScreenState> = combine(
        signInState,
        settingDs.getCurrentPlan,
        settingDs.getApiKey
    ) { signInState, currentPlan, apiKey ->
        MainScreenState(
            signInState = signInState,
            userPlan = currentPlan,
            apiKeyConfigured = apiKey != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainScreenState())

    val lifecycleObserver: LifecycleObserver = billingRepository

    val newPurchase = billingRepository.newPurchase

    fun updateLocalBillingState(newState: BillingRepository.SkuState) {
        billingRepository.updateLocalBillingState(newState)
    }

    fun requestSignIn(): Intent {
        updateSignInState(SignInState.Progress)
        return signInHandler.googleSignInClient.signInIntent
    }

    fun updateSignInState(value: SignInState) {
        signInState.update { value }
    }

    fun authWithFirebase(account: GoogleSignInAccount) {
        signInHandler.authWithFirebase(account) { success, idToken, errorMessage ->
            if (success && idToken != null) {
                viewModelScope.launch {
                    signInManager.getSignInStatus(tokenId = idToken)
                        .collectLatest {
                            val result = it.getOrNull()
                            if (result != null) {
                                updateSignInState(SignInState.Success)
                            } else {
                                updateSignInState(SignInState.Failure(errorMessage))
                            }
                        }
                }
            } else {
                // Guaranteed to have an error message
                Timber.d(errorMessage)
                updateSignInState(SignInState.Failure(errorMessage))
            }
        }
    }

    fun updateApiKeyLocally(key: String) {
        viewModelScope.launch {
            settingDs.updateApiKey(key)
        }
    }
}

sealed class SignInState {
    object Success : SignInState()
    data class Failure(val message: String?) : SignInState()
    object Unknown : SignInState()
    object Progress : SignInState()
}

data class MainScreenState(
    val signInState: SignInState = SignInState.Unknown,
    val userPlan: Plan? = null,
    val apiKeyConfigured: Boolean = false
)
