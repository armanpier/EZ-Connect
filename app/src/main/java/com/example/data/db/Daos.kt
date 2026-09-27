package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelDao {
    @Query("SELECT * FROM panels ORDER BY isCurrent DESC, id ASC")
    fun getAllPanels(): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panels WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentPanel(): Flow<PanelEntity?>

    @Query("SELECT * FROM panels WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentPanelSync(): PanelEntity?

    @Query("SELECT * FROM panels WHERE id = :id")
    suspend fun getPanelById(id: Long): PanelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanel(panel: PanelEntity): Long

    @Update
    suspend fun updatePanel(panel: PanelEntity)

    @Query("UPDATE panels SET isCurrent = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun setActivePanel(selectedId: Long)

    @Query("DELETE FROM panels WHERE id = :id")
    suspend fun deletePanel(id: Long)
}

@Dao
interface InboundDao {
    @Query("SELECT * FROM inbounds WHERE panelId = :panelId ORDER BY isFavorite DESC, id ASC")
    fun getInboundsByPanel(panelId: Long): Flow<List<InboundCacheEntity>>

    @Query("SELECT * FROM inbounds ORDER BY isFavorite DESC, id ASC")
    fun getAllInbounds(): Flow<List<InboundCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inbounds: List<InboundCacheEntity>)

    @Query("DELETE FROM inbounds WHERE panelId = :panelId")
    suspend fun deleteByPanel(panelId: Long)

    @Query("UPDATE inbounds SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT * FROM inbounds WHERE id = :id")
    suspend fun getInboundById(id: Long): InboundCacheEntity?
}

@Dao
interface VpnLogDao {
    @Query("SELECT * FROM vpn_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<VpnLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VpnLogEntity): Long

    @Query("DELETE FROM vpn_logs")
    suspend fun clearLogs()
}
