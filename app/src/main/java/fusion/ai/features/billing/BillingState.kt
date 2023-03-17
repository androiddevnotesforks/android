package fusion.ai.features.billing

import fusion.ai.billing.BillingRepository
import fusion.ai.billing.ProductPricing
import fusion.ai.util.ErrorEvent

data class BillingVMState(
    val enabled: Boolean = false,
    val summary: String = "Buy",
    val onClick: () -> Unit = {},
    val billingState: BillingRepository.SkuState = BillingRepository.SkuState.UNKNOWN,
    val billingPricing: ProductPricing? = null,
    val errorEvent: ErrorEvent? = null
)
