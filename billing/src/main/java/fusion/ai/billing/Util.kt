package fusion.ai.billing

enum class PurchaseType(
    val sku: String
) {
    Premium("premium");
    // Other purchase types may be added in the future here

    override fun toString() = name
}

enum class Plan(val token: String) {
    Monthly("monthly"),
    Trial("trial"),
    Tokens10K("10-k-tokens"),
    /** Users use their own API Key */
    ThreeMonthly("3-month-api-key");

    fun getPlanPurchaseToken() = token
}

data class ProductPricing(
    val monthly: String?,
    val threeMonthly: String?,
    val tokens10K: String?
)

fun Plan.toName(): String {
    return when (this) {
        Plan.ThreeMonthly -> "Three Month plan"
        Plan.Monthly -> "Monthly plan"
        Plan.Trial -> "Trial user"
        Plan.Tokens10K -> "10K Tokens"
    }
}

fun String.toPlan(): Plan {
    return when (this) {
        Plan.Monthly.token -> Plan.Monthly
        Plan.ThreeMonthly.token -> Plan.ThreeMonthly
        Plan.Tokens10K.token -> Plan.Tokens10K
        Plan.Trial.token -> Plan.Trial
        else -> throw IllegalAccessException("Plan Missing!")
    }
}
