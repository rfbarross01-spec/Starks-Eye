package com.lume.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: CaptureEntity): Long

    @Update
    suspend fun update(capture: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: Long): CaptureEntity?

    @Query("SELECT * FROM captures WHERE archived = 0 ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE archived = 0 ORDER BY timestampMs DESC")
    suspend fun getAllOnce(): List<CaptureEntity>

    @Query("""
        SELECT * FROM captures
        WHERE archived = 0 AND (
          tituloEvocativo LIKE :query
          OR observacaoAguda LIKE :query
          OR tipoConteudo LIKE :query
          OR vereditoUmaLinha LIKE :query
          OR tagsJson LIKE :query
        )
        ORDER BY timestampMs DESC
    """)
    fun search(query: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE starred = 1 AND archived = 0 ORDER BY timestampMs DESC")
    fun observeStarred(): Flow<List<CaptureEntity>>

    @Query("SELECT COUNT(*) FROM captures WHERE archived = 0")
    suspend fun countActive(): Int

    @Query("UPDATE captures SET starred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("UPDATE captures SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}
