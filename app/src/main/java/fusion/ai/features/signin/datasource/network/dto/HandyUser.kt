package fusion.ai.features.signin.datasource.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class HandyUserData(
    @SerializedName("uid")
    val uid: String,
    @SerializedName("dailyChatTokenLimit")
    val dailyChatTokenLimit: Int,
    @SerializedName("currentChatTokens")
    val currentChatTokens: Int,
    @SerializedName("imageGenerationToken")
    val imageGenerationToken: Int,
    @SerializedName("plan")
    val plan: String,
    @SerializedName("id")
    val id: Int
)

@Keep
data class HandyUser(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: HandyUserData?,
    @SerializedName("message")
    val message: String? = null
)
