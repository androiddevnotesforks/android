package fusion.ai.features.chat.datasource.network.request

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class OutgoingMessage(
    @SerializedName("content")
    val content: String,
    @SerializedName("toolId")
    val toolId: Int? = null,
    @SerializedName("extras")
    val extras: OutgoingExtras? = null
)

@Keep
data class OutgoingExtras(
    @SerializedName("id")
    val id: Int? = null
)
