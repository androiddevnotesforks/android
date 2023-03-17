package fusion.ai.features.signin.datasource.network.request

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class VerifyToken(
    @SerializedName("token")
    val token: String
)
