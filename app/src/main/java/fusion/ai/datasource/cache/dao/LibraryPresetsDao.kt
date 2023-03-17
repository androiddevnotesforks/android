package fusion.ai.datasource.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fusion.ai.datasource.cache.entity.LibraryPresetEntity

@Dao
interface LibraryPresetsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(tools: List<LibraryPresetEntity>)

    @Query("SELECT * FROM library_presets ORDER BY id DESC")
    suspend fun getPresets(): List<LibraryPresetEntity>

    @Query("DELETE from library_presets")
    suspend fun deleteAll()

    @Query("SELECT * FROM library_presets WHERE id = :id")
    suspend fun getPresetById(id: Int): LibraryPresetEntity?
}
