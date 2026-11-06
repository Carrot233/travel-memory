package com.carrot233.travelmemory.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlin.math.max

class TravelRepository(
    private val database: AppDatabase,
    private val media: MediaFiles,
) {
    private val importer = WebsiteZipImporter(database, media)

    fun observePhotos(): Flow<List<PhotoEntity>> = database.photoDao().observeAll()

    fun observePhoto(id: Long): Flow<PhotoEntity?> = database.photoDao().observe(id)

    fun observePhotoCount(): Flow<Int> = database.photoDao().observeCount()

    fun observeRoutes(): Flow<List<RouteEntity>> = database.routeDao().observeAll()

    fun observeRouteCount(): Flow<Int> = database.routeDao().observeCount()

    fun observeRoute(id: Long): Flow<RouteDetails?> = combine(
        database.routeDao().observe(id),
        database.routeDao().observeSpots(id),
        database.photoDao().observeForRoute(id),
    ) { route, spots, photos ->
        route?.let { RouteDetails(it, spots, photos) }
    }

    fun observeVideos(): Flow<List<VideoEntity>> = database.videoDao().observeAll()

    fun observeVideo(id: Long): Flow<VideoEntity?> = database.videoDao().observe(id)

    fun observeVideoCount(): Flow<Int> = database.videoDao().observeCount()

    fun observeStories(): Flow<List<StoryEntity>> = database.storyDao().observeAll()

    fun observeStory(id: Long): Flow<StoryEntity?> = database.storyDao().observe(id)

    fun observeStoryCount(): Flow<Int> = database.storyDao().observeCount()

    suspend fun getPhoto(id: Long): PhotoEntity? = database.photoDao().get(id)

    suspend fun getVideo(id: Long): VideoEntity? = database.videoDao().get(id)

    suspend fun getStory(id: Long): StoryEntity? = database.storyDao().get(id)

    suspend fun getRoute(id: Long): RouteDetails? {
        val route = database.routeDao().get(id) ?: return null
        return RouteDetails(route, database.routeDao().spots(id), emptyList())
    }

    suspend fun spotCount(routeId: Long): Int = database.routeDao().spotCount(routeId)

    suspend fun importMedia(context: Context, uri: Uri, kind: MediaKind): String = withContext(Dispatchers.IO) {
        copyUriIntoStorage(context, media, uri, kind)
    }

    suspend fun discardMedia(relative: String?) = withContext(Dispatchers.IO) {
        release(relative)
    }

    fun resolveMedia(relative: String?): java.io.File? = media.resolve(relative)

    suspend fun importWebsiteZip(context: Context, uri: Uri): ImportResult = importer.import(context, uri)

    suspend fun savePhoto(draft: PhotoDraft): Long {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "请填写标题" }
        require(draft.url.isNotBlank()) { "请选择一张照片" }
        val description = draft.description.blankToNull()
        val location = draft.location.blankToNull()
        val takenAt = draft.takenAt.blankToNull()
        if (draft.id == null) {
            return database.photoDao().insert(
                PhotoEntity(
                    title = title,
                    description = description,
                    url = draft.url,
                    location = location,
                    routeId = draft.routeId,
                    takenAt = takenAt,
                    createdAt = nowStamp(),
                ),
            )
        }
        val existing = database.photoDao().get(draft.id) ?: error("照片不存在")
        database.photoDao().update(
            existing.copy(
                title = title,
                description = description,
                url = draft.url,
                location = location,
                routeId = draft.routeId,
                takenAt = takenAt,
            ),
        )
        if (existing.url != draft.url) release(existing.url)
        return draft.id
    }

    suspend fun deletePhoto(photo: PhotoEntity) {
        database.photoDao().delete(photo)
        release(photo.url)
    }

    suspend fun saveVideo(draft: VideoDraft): Long {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "请填写标题" }
        require(draft.url.isNotBlank()) { "请选择一个视频" }
        val description = draft.description.blankToNull()
        val file = media.resolve(draft.url)
        if (draft.id == null) {
            val (seconds, thumbnail) = file?.let { VideoMetadataProbe.read(it, media) } ?: (null to null)
            return database.videoDao().insert(
                VideoEntity(
                    title = title,
                    description = description,
                    url = draft.url,
                    thumbnailUrl = thumbnail,
                    duration = seconds,
                    createdAt = nowStamp(),
                ),
            )
        }
        val existing = database.videoDao().get(draft.id) ?: error("视频不存在")
        var duration = existing.duration
        var thumbnail = existing.thumbnailUrl
        if (existing.url != draft.url && file != null) {
            val probed = VideoMetadataProbe.read(file, media)
            duration = probed.first ?: duration
            if (probed.second != null) thumbnail = probed.second
        }
        database.videoDao().update(
            existing.copy(
                title = title,
                description = description,
                url = draft.url,
                thumbnailUrl = thumbnail,
                duration = duration,
            ),
        )
        if (existing.url != draft.url) release(existing.url)
        if (existing.thumbnailUrl != thumbnail) release(existing.thumbnailUrl)
        return draft.id
    }

    suspend fun deleteVideo(video: VideoEntity) {
        database.videoDao().delete(video)
        release(video.url)
        release(video.thumbnailUrl)
    }

    suspend fun saveStory(draft: StoryDraft): Long {
        val title = draft.title.trim()
        val content = draft.content.trim()
        require(title.isNotEmpty()) { "请填写标题" }
        require(content.isNotEmpty()) { "请写下故事内容" }
        val location = draft.location.blankToNull()
        val cover = draft.coverUrl.blankToNull()
        if (draft.id == null) {
            return database.storyDao().insert(
                StoryEntity(
                    title = title,
                    content = content,
                    coverUrl = cover,
                    location = location,
                    createdAt = nowStamp(),
                ),
            )
        }
        val existing = database.storyDao().get(draft.id) ?: error("故事不存在")
        database.storyDao().update(
            existing.copy(
                title = title,
                content = content,
                coverUrl = cover,
                location = location,
            ),
        )
        if (existing.coverUrl != cover) release(existing.coverUrl)
        return draft.id
    }

    suspend fun deleteStory(story: StoryEntity) {
        database.storyDao().delete(story)
        release(story.coverUrl)
    }

    suspend fun saveRoute(draft: RouteDraft): Long = database.withTransaction {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "请填写标题" }
        val spots = draft.spots.filter { it.name.isNotBlank() }
        val spotDays = spots.maxOfOrNull { it.dayNumber.coerceAtLeast(1) } ?: 1
        val days = max(draft.days.coerceAtLeast(1), spotDays)
        val description = draft.description.blankToNull()
        val cover = draft.coverUrl.blankToNull()
        val routeId = if (draft.id == null) {
            database.routeDao().insert(
                RouteEntity(
                    title = title,
                    description = description,
                    coverUrl = cover,
                    days = days,
                    createdAt = nowStamp(),
                ),
            )
        } else {
            val existing = database.routeDao().get(draft.id) ?: error("路线不存在")
            database.routeDao().update(
                existing.copy(
                    title = title,
                    description = description,
                    coverUrl = cover,
                    days = days,
                ),
            )
            if (existing.coverUrl != cover) release(existing.coverUrl)
            draft.id
        }
        val existingSpots = database.routeDao().spots(routeId)
        val kept = spots.mapNotNull { it.id }.toSet()
        existingSpots.filter { it.id !in kept }.forEach { database.routeDao().deleteSpot(it) }
        spots.forEachIndexed { index, spot ->
            val day = spot.dayNumber.coerceIn(1, days)
            if (spot.id == null) {
                database.routeDao().insertSpot(
                    RouteSpotEntity(
                        routeId = routeId,
                        name = spot.name.trim(),
                        description = spot.description.blankToNull(),
                        latitude = spot.latitude,
                        longitude = spot.longitude,
                        dayNumber = day,
                        orderIndex = index,
                    ),
                )
            } else {
                val current = existingSpots.firstOrNull { it.id == spot.id } ?: return@forEachIndexed
                database.routeDao().updateSpot(
                    current.copy(
                        name = spot.name.trim(),
                        description = spot.description.blankToNull(),
                        latitude = spot.latitude,
                        longitude = spot.longitude,
                        dayNumber = day,
                        orderIndex = index,
                    ),
                )
            }
        }
        routeId
    }

    suspend fun deleteRoute(id: Long) {
        val route = database.routeDao().get(id) ?: return
        database.routeDao().delete(route)
        release(route.coverUrl)
    }

    private suspend fun release(path: String?) {
        val relative = path.blankToNull() ?: return
        if (database.storageDao().referenceCount(relative) > 0) return
        media.deleteOwned(relative)
    }
}
