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
    /** Users use their own API Key */
    ThreeMonthly("3-month-api-key"),
    Trial("Trial")
}

val planList = listOf(Plan.Monthly, Plan.ThreeMonthly)

fun ProductPricing.getAccordingly(plan: Plan): String? {
    return when (plan) {
        Plan.Monthly -> monthly
        Plan.ThreeMonthly -> threeMonthly
        Plan.Trial -> null
    }
}

data class ProductPricing(
    val monthly: String?,
    val threeMonthly: String?,
)

fun Plan.toName(): String {
    return when (this) {
        Plan.ThreeMonthly -> "Three Month plan"
        Plan.Monthly -> "Monthly plan"
        Plan.Trial -> "Trial user"
    }
}

fun String.toPlan(): Plan {
    return when (this) {
        Plan.Monthly.token -> Plan.Monthly
        Plan.ThreeMonthly.token -> Plan.ThreeMonthly
        else -> Plan.Trial
    }
}
