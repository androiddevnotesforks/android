package fusion.ai.datasource.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import fusion.ai.datasource.cache.dao.ChatDao
import fusion.ai.datasource.cache.dao.LibraryPresetsDao
import fusion.ai.datasource.cache.entity.ChatEntity
import fusion.ai.datasource.cache.entity.LibraryPresetEntity
import fusion.ai.datasource.cache.entity.LibraryToolEntity
import fusion.ai.datasource.cache.entity.LibraryToolsExtraEntity
import fusion.ai.datasource.cache.entity.MessageEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        LibraryPresetEntity::class,
        LibraryToolEntity::class,
        LibraryToolsExtraEntity::class
    ],
    version = 1
)
@TypeConverters(ListConverter::class)
abstract class Database : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun libraryPresetsDao(): LibraryPresetsDao
}

@TypeConverters
object ListConverter {
    @TypeConverter
    fun fromList(list: List<LibraryToolsExtraEntity>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun toList(json: String): List<LibraryToolsExtraEntity> {
        return Json.decodeFromString(json)
    }

    @TypeConverter
    fun fromLibraryToolList(list: List<LibraryToolEntity>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun toLibraryToolList(json: String): List<LibraryToolEntity> {
        return Json.decodeFromString(json)
    }
}
