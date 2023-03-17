package fusion.ai.features.billing.datasource.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class VerifyPurchaseBody(
    @SerializedName("purchase_token")
    val purchaseToken: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("productId")
    val productId: String
)
