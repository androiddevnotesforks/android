package fusion.ai.features.billing

import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
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

fun ProductPricing.getAccordingly(plan: Plan): String? {
    return when (plan) {
        Plan.Monthly -> monthly
        Plan.ThreeMonthly -> threeMonthly
        Plan.Tokens10K -> tokens10K
        else -> null
    }
}
