package fusion.ai.util

import androidx.annotation.StringRes
import java.util.UUID

data class ErrorEvent(
    val id: String = UUID.randomUUID().toString(),
    @StringRes val message: Int
)
