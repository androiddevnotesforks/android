package fusion.ai.billing

enum class PurchaseType(
    val sku: String
) {
    Premium("premium"), Lifetime("10_k_tokens");
    // Other purchase types may be added in the future here

    override fun toString() = name
}

enum class Plan(val id: String) {
    Monthly("premium"), Lifetime("10_k_tokens"), Trial("Trial")
}

val planList = listOf(Plan.Monthly, Plan.Lifetime)

fun ProductPricing.getAccordingly(plan: Plan): String? {
    return when (plan) {
        Plan.Monthly -> monthly
        Plan.Lifetime -> lifetime
        Plan.Trial -> null
    }
}

data class ProductPricing(
    val monthly: String?,
    val lifetime: String?,
)

const val MonthlyToken = "monthly"

fun Plan.toName(): String {
    return when (this) {
        Plan.Lifetime -> "Lifetime access"
        Plan.Monthly -> "Monthly plan"
        Plan.Trial -> "Trial user"
    }
}
