package fusion.ai.features.billing.datasource.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import fusion.ai.features.signin.datasource.network.dto.HandyUser

@Keep
data class VerifyPurchaseResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val handyUser: HandyUser?
)
