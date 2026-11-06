package com.carrot233.travelmemory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    fun observe(id: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun get(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE route_id = :routeId ORDER BY created_at DESC")
    fun observeForRoute(routeId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos")
    fun observeCount(): Flow<Int>

    @Query("SELECT id FROM photos WHERE source_key = :key LIMIT 1")
    suspend fun idBySourceKey(key: String): Long?

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Update
    suspend fun update(photo: PhotoEntity)

    @Delete
    suspend fun delete(photo: PhotoEntity)
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun observe(id: Long): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun get(id: Long): RouteEntity?

    @Query("SELECT * FROM routes WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<RouteEntity>>

    @Query("SELECT COUNT(*) FROM routes")
    fun observeCount(): Flow<Int>

    @Query("SELECT id FROM routes WHERE source_key = :key LIMIT 1")
    suspend fun idBySourceKey(key: String): Long?

    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query(
        "SELECT * FROM route_spots WHERE route_id = :routeId " +
            "ORDER BY day_number ASC, order_index ASC, id ASC",
    )
    fun observeSpots(routeId: Long): Flow<List<RouteSpotEntity>>

    @Query(
        "SELECT * FROM route_spots WHERE route_id = :routeId " +
            "ORDER BY day_number ASC, order_index ASC, id ASC",
    )
    suspend fun spots(routeId: Long): List<RouteSpotEntity>

    @Query("SELECT COUNT(*) FROM route_spots WHERE route_id = :routeId")
    suspend fun spotCount(routeId: Long): Int

    @Query("SELECT id FROM route_spots WHERE source_key = :key LIMIT 1")
    suspend fun spotIdBySourceKey(key: String): Long?

    @Insert
    suspend fun insertSpot(spot: RouteSpotEntity): Long

    @Update
    suspend fun updateSpot(spot: RouteSpotEntity)

    @Delete
    suspend fun deleteSpot(spot: RouteSpotEntity)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY created_at DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun observe(id: Long): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun get(id: Long): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    fun observeCount(): Flow<Int>

    @Query("SELECT id FROM videos WHERE source_key = :key LIMIT 1")
    suspend fun idBySourceKey(key: String): Long?

    @Insert
    suspend fun insert(video: VideoEntity): Long

    @Update
    suspend fun update(video: VideoEntity)

    @Delete
    suspend fun delete(video: VideoEntity)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY created_at DESC")
    fun observeAll(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    fun observe(id: Long): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun get(id: Long): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories")
    fun observeCount(): Flow<Int>

    @Query("SELECT id FROM stories WHERE source_key = :key LIMIT 1")
    suspend fun idBySourceKey(key: String): Long?

    @Insert
    suspend fun insert(story: StoryEntity): Long

    @Update
    suspend fun update(story: StoryEntity)

    @Delete
    suspend fun delete(story: StoryEntity)
}

@Dao
interface StorageDao {
    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM photos WHERE url = :path) +
          (SELECT COUNT(*) FROM routes WHERE cover_url = :path) +
          (SELECT COUNT(*) FROM videos WHERE url = :path OR thumbnail_url = :path) +
          (SELECT COUNT(*) FROM stories WHERE cover_url = :path)
        """,
    )
    suspend fun referenceCount(path: String): Int
}
