package fusion.ai.features.chat.datasource.network.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import fusion.ai.datasource.cache.entity.LibraryToolEntity
import fusion.ai.datasource.cache.entity.LibraryToolsExtraEntity

@Keep
data class LibraryPresetDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("tools")
    val tools: List<LibraryToolDto>
)

@Keep
data class LibraryToolDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("iconUrl")
    val iconUrl: String? = null,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("locked")
    val locked: Boolean = false,
    @SerializedName("placeholder")
    val placeholder: String? = null,
    @SerializedName("comingSoon")
    val comingSoon: Boolean = false,
    @SerializedName("extras")
    val extras: List<LibraryToolExtrasDto> = emptyList(),
    @SerializedName("extrasHeader")
    val extrasHeader: String? = null,
    @SerializedName("showOnMainScreen")
    val showOnMainScreen: Boolean
) {
    fun toEntity(): LibraryToolEntity {
        return LibraryToolEntity(
            id = id,
            iconUrl = iconUrl,
            title = title,
            description = description,
            locked = locked,
            placeholder = placeholder,
            comingSoon = comingSoon,
            extras = extras.map { LibraryToolsExtraEntity(id = it.id, title = it.title) },
            extrasHeader = extrasHeader,
            showOnMainScreen = showOnMainScreen
        )
    }
}

@Keep
data class LibraryToolExtrasDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String
)
