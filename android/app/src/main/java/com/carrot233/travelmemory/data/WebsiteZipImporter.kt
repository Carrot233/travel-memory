package com.carrot233.travelmemory.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipException
import java.util.zip.ZipFile

private const val MAX_ZIP_BYTES = 2L * 1024 * 1024 * 1024
private const val MAX_DB_BYTES = 512L * 1024 * 1024
private const val MAX_MEDIA_BYTES = 1536L * 1024 * 1024

class WebsiteZipImporter(
    private val database: AppDatabase,
    private val media: MediaFiles,
) {
    suspend fun import(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "website-import").apply { mkdirs() }
        val zipCopy = File(cacheDir, "package-${System.nanoTime()}.zip")
        val dbCopy = File(cacheDir, "travel-${System.nanoTime()}.db")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                zipCopy.outputStream().use { output ->
                    copyLimited(input, output, MAX_ZIP_BYTES) || error("压缩包太大，已停止导入")
                }
            } ?: throw IllegalArgumentException("无法读取所选文件")

            try {
                ZipFile(zipCopy, StandardCharsets.UTF_8).use { zip ->
                    val names = zip.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList()
                    val dbName = chooseDatabaseEntry(names)
                        ?: throw IllegalArgumentException("压缩包里没有 travel.db")
                    val dbEntry = zip.getEntry(dbName) ?: throw IllegalArgumentException("压缩包里没有 travel.db")
                    zip.getInputStream(dbEntry).use { input ->
                        dbCopy.outputStream().use { output ->
                            copyLimited(input, output, MAX_DB_BYTES) || error("travel.db 太大，已停止导入")
                        }
                    }
                    val sqlite = try {
                        SQLiteDatabase.openDatabase(
                            dbCopy.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READONLY,
                        )
                    } catch (error: RuntimeException) {
                        throw IllegalArgumentException("无法读取 travel.db，它不是可用的 SQLite 数据库")
                    }
                    try {
                        val dump = readWebsiteDump(AndroidTabularDb(sqlite))
                        if (dump.isEmpty) {
                            throw IllegalArgumentException("数据包里没有照片、路线、视频或故事")
                        }
                        apply(zip, names, dump)
                    } finally {
                        sqlite.close()
                    }
                }
            } catch (error: ZipException) {
                throw IllegalArgumentException("这不是有效的 zip 文件")
            }
        } finally {
            zipCopy.delete()
            dbCopy.delete()
        }
    }

    private suspend fun apply(zip: ZipFile, names: List<String>, dump: WebsiteDump): ImportResult {
        var photos = 0
        var routes = 0
        var spots = 0
        var videos = 0
        var stories = 0
        var skipped = 0
        var missing = 0
        val routeIds = HashMap<Long, Long>()
        val spotIds = HashMap<Long, Long>()
        val photoIds = HashMap<Long, Long>()

        database.withTransaction {
            for (route in dump.routes) {
                val existing = database.routeDao().idBySourceKey(route.sourceKey)
                if (existing != null) {
                    routeIds[route.oldId] = existing
                    skipped++
                    continue
                }
                val cover = copyMedia(zip, names, route.sourceKey, route.coverUrl, MediaKind.COVER)
                if (route.coverUrl != null && cover == null) missing++
                val id = database.routeDao().insert(
                    RouteEntity(
                        title = route.title,
                        description = route.description,
                        coverUrl = cover,
                        days = route.days.coerceAtLeast(1),
                        createdAt = route.createdAt ?: nowStamp(),
                        sourceKey = route.sourceKey,
                    ),
                )
                routeIds[route.oldId] = id
                routes++
            }

            for (spot in dump.spots) {
                val existing = database.routeDao().spotIdBySourceKey(spot.sourceKey)
                if (existing != null) {
                    spotIds[spot.oldId] = existing
                    skipped++
                    continue
                }
                val routeId = routeIds[spot.oldRouteId]
                    ?: database.routeDao().idBySourceKey("website:route:${spot.oldRouteId}")
                if (routeId == null) {
                    skipped++
                    continue
                }
                val id = database.routeDao().insertSpot(
                    RouteSpotEntity(
                        routeId = routeId,
                        name = spot.name,
                        description = spot.description,
                        latitude = spot.latitude,
                        longitude = spot.longitude,
                        dayNumber = spot.dayNumber.coerceAtLeast(1),
                        orderIndex = spot.orderIndex,
                        sourceKey = spot.sourceKey,
                    ),
                )
                spotIds[spot.oldId] = id
                spots++
            }

            for (photo in dump.photos) {
                val existingPhoto = database.photoDao().idBySourceKey(photo.sourceKey)
                if (existingPhoto != null) {
                    photoIds[photo.oldId] = existingPhoto
                    skipped++
                    continue
                }
                val copied = copyMedia(zip, names, photo.sourceKey, photo.url, MediaKind.PHOTO)
                if (photo.url != null && copied == null) missing++
                val id = database.photoDao().insert(
                    PhotoEntity(
                        title = photo.title,
                        description = photo.description,
                        url = copied ?: "",
                        location = photo.location,
                        routeId = photo.oldRouteId?.let { routeIds[it] },
                        spotId = photo.oldSpotId?.let { spotIds[it] },
                        takenAt = photo.takenAt,
                        createdAt = photo.createdAt ?: nowStamp(),
                        sourceKey = photo.sourceKey,
                    ),
                )
                photoIds[photo.oldId] = id
                photos++
            }

            for (video in dump.videos) {
                if (database.videoDao().idBySourceKey(video.sourceKey) != null) {
                    skipped++
                    continue
                }
                val copied = copyMedia(zip, names, video.sourceKey, video.url, MediaKind.VIDEO)
                if (copied == null) {
                    missing++
                    continue
                }
                val thumb = copyMedia(zip, names, video.sourceKey + "-thumb", video.thumbnailUrl, MediaKind.THUMBNAIL)
                if (video.thumbnailUrl != null && thumb == null) missing++
                database.videoDao().insert(
                    VideoEntity(
                        title = video.title,
                        description = video.description,
                        url = copied,
                        thumbnailUrl = thumb,
                        duration = video.duration,
                        createdAt = video.createdAt ?: nowStamp(),
                        sourceKey = video.sourceKey,
                    ),
                )
                videos++
            }

            for (story in dump.stories) {
                val linkedPhotos = linkedIds(story.photoIds, photoIds, "website:photo:")
                val linkedRoutes = linkedIds(story.routeIds, routeIds, "website:route:")
                val existingId = database.storyDao().idBySourceKey(story.sourceKey)
                if (existingId != null) {
                    val existing = database.storyDao().get(existingId)
                    if (existing != null) {
                        database.storyDao().update(
                            existing.copy(photoIds = linkedPhotos, routeIds = linkedRoutes),
                        )
                    }
                    skipped++
                    continue
                }
                val cover = copyMedia(zip, names, story.sourceKey, story.coverUrl, MediaKind.COVER)
                if (story.coverUrl != null && cover == null) missing++
                database.storyDao().insert(
                    StoryEntity(
                        title = story.title,
                        content = story.content,
                        coverUrl = cover,
                        location = story.location,
                        photoIds = linkedPhotos,
                        routeIds = linkedRoutes,
                        createdAt = story.createdAt ?: nowStamp(),
                        sourceKey = story.sourceKey,
                    ),
                )
                stories++
            }
        }

        return ImportResult(photos, routes, spots, videos, stories, skipped, missing)
    }

    private suspend fun linkedIds(oldIds: List<Long>, known: Map<Long, Long>, sourcePrefix: String): String? {
        val local = oldIds.mapNotNull { oldId ->
            known[oldId] ?: when {
                sourcePrefix.endsWith("photo:") -> database.photoDao().idBySourceKey("$sourcePrefix$oldId")
                sourcePrefix.endsWith("route:") -> database.routeDao().idBySourceKey("$sourcePrefix$oldId")
                else -> null
            }
        }
        return encodeIdList(local)
    }

    private fun copyMedia(
        zip: ZipFile,
        names: List<String>,
        sourceKey: String,
        url: String?,
        kind: MediaKind,
    ): String? {
        val entryName = matchZipEntry(names, url) ?: return null
        val entry = zip.getEntry(entryName) ?: return null
        if (entry.size > MAX_MEDIA_BYTES) return null
        val relative = importedRelativePath(kind.folder, sourceKey, entryName)
        val dest = media.resolve(relative) ?: return null
        dest.parentFile?.mkdirs()
        return try {
            zip.getInputStream(entry).use { input ->
                dest.outputStream().use { output ->
                    if (!copyLimited(input, output, MAX_MEDIA_BYTES)) {
                        dest.delete()
                        return null
                    }
                }
            }
            relative
        } catch (error: RuntimeException) {
            dest.delete()
            null
        }
    }
}

private class AndroidTabularDb(private val database: SQLiteDatabase) : TabularDb {
    override fun query(sql: String): List<Row> {
        database.rawQuery(sql, null).use { cursor ->
            val names = cursor.columnNames
            val rows = ArrayList<Row>(cursor.count.coerceAtLeast(0))
            while (cursor.moveToNext()) {
                val values = LinkedHashMap<String, String?>(names.size)
                for (index in names.indices) {
                    values[names[index]] = if (cursor.isNull(index)) null else cursor.getString(index)
                }
                rows += Row(values)
            }
            return rows
        }
    }
}

internal fun copyLimited(input: InputStream, output: OutputStream, maxBytes: Long): Boolean {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return true
        total += read
        if (total > maxBytes) return false
        output.write(buffer, 0, read)
    }
}
