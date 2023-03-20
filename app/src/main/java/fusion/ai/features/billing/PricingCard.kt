@file:OptIn(ExperimentalMaterial3Api::class)

package fusion.ai.features.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fusion.ai.billing.Plan
import fusion.ai.billing.ProductPricing
import fusion.ai.billing.getAccordingly
import fusion.ai.ui.theme.InterFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingCard(
    modifier: Modifier = Modifier,
    plan: Plan,
    isSelected: Boolean,
    updateSelection: () -> Unit,
    productPricing: ProductPricing?,
    isEnabled: Boolean
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        onClick = updateSelection,
        border = BorderStroke(
            width = if (isSelected) 2.dp else .5.dp,
            color = MaterialTheme.colorScheme.primary.copy(
                alpha = if (isSelected) 1f else .5f
            )
        ),
        enabled = isEnabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = plan.getName(),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = .5.sp,
                modifier = Modifier.padding(5.dp),
                textAlign = TextAlign.Center
            )

            productPricing?.let {
                Text(
                    text = productPricing.getAccordingly(plan) ?: "Unknown",
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

fun Plan.getName(): String {
    return when (this) {
        Plan.Monthly -> "Unlimited Monthly Subscription"
        Plan.ThreeMonthly -> "Your Own API Key + 3 Month Subscription**"
        Plan.Trial -> "Trial Plan"
    }
}

fun Plan.getRewards(): List<Features> {
    return when (this) {
        Plan.Monthly -> listOf(
            Features("Unlimited conversation"),
            Features("Fast & Secure"),
            Features("Access to all presets in library"),
            Features("GPT remembers your previous 3 chats"),
            Features("25 Image Generation")
        )

        Plan.ThreeMonthly -> listOf(
            Features("You pay directly to OpenAI\n(1000s of Chat is ~ $1)"),
            Features("Fast & Secure"),
            Features("Access to all presets in library"),
            Features("GPT remembers your previous 10 chats"),
            Features("50 Image Generation"),
            Features("Send and receive up-to 4000 words")
        )

        else -> listOf()
    }
}

data class Features(
    val text: String,
    val isAvailable: Boolean = true
) {
    companion object {
        val Features.reformedText get() = if (!isAvailable) "$text (Coming Soon)" else text
    }
}
