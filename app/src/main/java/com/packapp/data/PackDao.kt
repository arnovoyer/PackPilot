package com.packapp.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PackedHistoryEntry(
    val itemTitle: String,
    val listName: String,
    val packedAt: Long
)

data class PackingListSummary(
    @Embedded val list: PackingListEntity,
    val totalCount: Int,
    val packedCount: Int,
    val lastActivityAt: Long
)

@Dao
interface PackDao {
    @Query(
        """
        SELECT
            packing_lists.id AS id,
            packing_lists.name AS name,
            packing_lists.createdAt AS createdAt,
            packing_lists.tripId AS tripId,
            COUNT(packing_items.id) AS totalCount,
            SUM(CASE WHEN packing_items.isPacked = 1 THEN 1 ELSE 0 END) AS packedCount,
            COALESCE(
                MAX(
                    CASE
                        WHEN packing_items.packedAt IS NOT NULL THEN packing_items.packedAt
                        ELSE packing_items.createdAt
                    END
                ),
                packing_lists.createdAt
            ) AS lastActivityAt
        FROM packing_lists
        LEFT JOIN packing_items ON packing_items.listId = packing_lists.id
        GROUP BY packing_lists.id
        """
    )
    fun observeListSummaries(): Flow<List<PackingListSummary>>

    @Query("SELECT * FROM packing_lists ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<PackingListEntity>>

    @Query("SELECT * FROM packing_lists WHERE id = :listId LIMIT 1")
    fun observeList(listId: Long): Flow<PackingListEntity?>

    @Insert
    suspend fun insertList(list: PackingListEntity): Long

    @Update
    suspend fun updateList(list: PackingListEntity)

    @Query("DELETE FROM packing_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Query(
        """
        SELECT * FROM packing_items
        WHERE listId = :listId
        ORDER BY isPacked ASC,
        CASE WHEN isPacked = 0 THEN createdAt ELSE COALESCE(packedAt, createdAt) END ASC
        """
    )
    fun observeItemsForList(listId: Long): Flow<List<PackingItemEntity>>

    @Insert
    suspend fun insertItem(item: PackingItemEntity)

    @Query("UPDATE packing_items SET isPacked = 1, packedAt = :packedAt WHERE id = :itemId")
    suspend fun markPacked(itemId: Long, packedAt: Long)

    @Query("UPDATE packing_items SET isPacked = 0, packedAt = NULL WHERE id = :itemId")
    suspend fun markUnpacked(itemId: Long)

    @Query("UPDATE packing_items SET isPacked = 0, packedAt = NULL WHERE listId = :listId")
    suspend fun resetListItems(listId: Long)

    @Query("SELECT COUNT(*) FROM packing_items WHERE listId = :listId")
    fun observeTotalCount(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_items WHERE listId = :listId AND isPacked = 1")
    fun observePackedCount(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_lists")
    fun observeListCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_items")
    fun observeItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_items WHERE isPacked = 1")
    fun observePackedItemCountTotal(): Flow<Int>

    @Insert
    suspend fun insertActivityEvent(event: ActivityEventEntity)

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentActivityEvents(limit: Int): Flow<List<ActivityEventEntity>>

    @Query("SELECT COUNT(*) FROM activity_events")
    fun observeActivityEventCount(): Flow<Int>

    @Query("SELECT * FROM packing_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Long): PackingListEntity?

    @Query("SELECT * FROM packing_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): PackingItemEntity?

    @Query(
        """
        SELECT packing_items.title AS itemTitle, packing_lists.name AS listName, packing_items.packedAt AS packedAt
        FROM packing_items
        INNER JOIN packing_lists ON packing_lists.id = packing_items.listId
        WHERE packing_items.packedAt IS NOT NULL
        ORDER BY packing_items.packedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentPackedItems(limit: Int): Flow<List<PackedHistoryEntry>>

    // ===== PACKING SESSION QUERIES =====
    @Insert
    suspend fun insertPackingSession(session: PackingSessionEntity): Long

    @Update
    suspend fun updatePackingSession(session: PackingSessionEntity)

    @Query("SELECT * FROM packing_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): PackingSessionEntity?

    @Query("SELECT * FROM packing_sessions WHERE listId = :listId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSessionForList(listId: Long): PackingSessionEntity?

    @Query(
        """
        SELECT * FROM packing_sessions
        WHERE listId = :listId
        ORDER BY startTime DESC
        """
    )
    fun observeSessionsForList(listId: Long): Flow<List<PackingSessionEntity>>

    @Query("SELECT MIN(durationMillis) FROM (SELECT (COALESCE(endTime, :now) - startTime) AS durationMillis FROM packing_sessions WHERE listId = :listId AND endTime IS NOT NULL)")
    suspend fun getFastestSessionDuration(listId: Long, now: Long = System.currentTimeMillis()): Long?

    @Query(
        """
        SELECT AVG(COALESCE(endTime, :now) - startTime) as avgDurationMillis
        FROM packing_sessions
        WHERE listId = :listId AND endTime IS NOT NULL
        """
    )
    suspend fun getAverageSessionDuration(listId: Long, now: Long = System.currentTimeMillis()): Long?

    // ===== TRIP QUERIES =====
    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Query("SELECT COUNT(*) FROM trips")
    fun observeTripCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT location) FROM trips WHERE location != ''")
    fun observeUniqueLocationsCount(): Flow<Int>

    // ===== WEIGHT QUERIES =====
    @Query("SELECT SUM(weightGrams) FROM packing_items WHERE listId = :listId AND isPacked = 1")
    fun observePackedWeightForList(listId: Long): Flow<Int>

    @Query("SELECT SUM(weightGrams) FROM packing_items WHERE listId = :listId")
    fun observeTotalWeightForList(listId: Long): Flow<Int>

    // ===== EFFICIENCY QUERIES =====
    @Query(
        """
        SELECT COUNT(*) FROM packing_items
        WHERE listId = :listId AND isPacked = 1 AND wasUsed = 1
        """
    )
    fun observeUsedItemsCount(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_items WHERE isPacked = 1 AND wasUsed = 1")
    fun observeUsedPackedItemCountTotal(): Flow<Int>

    @Query("SELECT MIN(endTime - startTime) FROM packing_sessions WHERE endTime IS NOT NULL")
    fun observeFastestSessionDurationMillis(): Flow<Long?>

    @Query("SELECT AVG(endTime - startTime) FROM packing_sessions WHERE endTime IS NOT NULL")
    fun observeAverageSessionDurationMillis(): Flow<Double?>
}
