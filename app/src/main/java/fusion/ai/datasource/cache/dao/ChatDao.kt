package fusion.ai.datasource.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import fusion.ai.datasource.cache.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("SELECT * FROM chat ORDER BY timestamp DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("SELECT * FROM chat WHERE chatId = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Query("DELETE FROM chat WHERE chatId = :id")
    suspend fun deleteChat(id: String)
}
