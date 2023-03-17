package fusion.ai.datasource.cache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "library_tools")
data class LibraryToolEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val iconUrl: String? = null,
    val title: String,
    val description: String,
    val locked: Boolean,
    val placeholder: String? = null,
    val comingSoon: Boolean = false,
    val extras: List<LibraryToolsExtraEntity>,
    val extrasHeader: String? = null,
    val showOnMainScreen: Boolean
)

@Entity(tableName = "library_presets")
data class LibraryPresetEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val title: String,
    val tools: List<LibraryToolEntity>
)

@Serializable
@Entity(tableName = "chat_tools_extras")
data class LibraryToolsExtraEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val title: String
)
