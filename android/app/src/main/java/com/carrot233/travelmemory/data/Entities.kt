package com.carrot233.travelmemory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RouteSpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spot_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("route_id"),
        Index("spot_id"),
        Index(value = ["source_key"], unique = true),
    ],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val url: String,
    val location: String? = null,
    @ColumnInfo(name = "route_id") val routeId: Long? = null,
    @ColumnInfo(name = "spot_id") val spotId: Long? = null,
    @ColumnInfo(name = "taken_at") val takenAt: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "source_key") val sourceKey: String? = null,
)

@Entity(
    tableName = "routes",
    indices = [Index(value = ["source_key"], unique = true)],
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    val days: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "source_key") val sourceKey: String? = null,
)

@Entity(
    tableName = "route_spots",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("route_id"),
        Index(value = ["source_key"], unique = true),
    ],
)
data class RouteSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "route_id") val routeId: Long,
    val name: String,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ColumnInfo(name = "day_number") val dayNumber: Int = 1,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
    @ColumnInfo(name = "source_key") val sourceKey: String? = null,
)

@Entity(
    tableName = "videos",
    indices = [Index(value = ["source_key"], unique = true)],
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val url: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    val duration: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "source_key") val sourceKey: String? = null,
)

@Entity(
    tableName = "stories",
    indices = [Index(value = ["source_key"], unique = true)],
)
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    val location: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "source_key") val sourceKey: String? = null,
)
