package fusion.ai.features.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import fusion.ai.BuildConfig
import fusion.ai.R
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
import fusion.ai.billing.ProductPricing
import fusion.ai.billing.PurchaseType
import fusion.ai.util.ErrorEvent
import fusion.ai.util.combineFlows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BillingVM @Inject constructor(
    private val billingRepository: BillingRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val enabled = MutableStateFlow(false)
    private val summary = MutableStateFlow("Loading...")
    private val onClick = MutableStateFlow {}

    private val errorEvent = MutableStateFlow<ErrorEvent?>(null)

    val billingState: StateFlow<BillingVMState> = combineFlows(
        enabled,
        summary,
        onClick,
        merge(
            billingRepository.localBillingState,
            billingRepository.subscriptionState,
            billingRepository.lifetimePurchaseState
        ),
        billingRepository.monthlyPrice,
        billingRepository.lifetimePrice,
        errorEvent
    ) { enabled, summary, onClick, subscriptionState, monthlyPricing, lifetimePricing, errorEvent ->
        BillingVMState(
            enabled = enabled,
            summary = summary,
            onClick = onClick,
            billingState = subscriptionState,
            billingPricing = ProductPricing(
                monthly = monthlyPricing,
                lifetime = lifetimePricing
            ),
            errorEvent = errorEvent
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BillingVMState()
    )

    fun updateOnClick(function: () -> Unit) {
        onClick.update { function }
    }

    fun updateSummary(text: String) {
        summary.update { text }
    }

    fun updateEnable(value: Boolean) {
        enabled.update { value }
    }

    /**
     * Starts a billing flow for purchasing [type].
     *
     * @return whether or not we were able to start the flow
     */
    fun makePurchase(
        activity: Activity,
        type: PurchaseType,
        plan: Plan
    ) {
        if (firebaseAuth.currentUser == null) {
            errorEvent.tryEmit(ErrorEvent(message = R.string.sign_in_required))
        } else {
            try {
                billingRepository.makePurchase(activity, type.sku, plan)
            } catch (e: Exception) {
                Timber.e(e)
                errorEvent.tryEmit(ErrorEvent(message = R.string.purchase_error_exception))
            }
        }
    }

    fun consumePurchaseDebug() {
        if (BuildConfig.DEBUG) {
            billingRepository.debugConsume()
        }
    }
}
