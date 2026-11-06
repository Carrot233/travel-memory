package com.carrot233.travelmemory.data

data class Row(val values: Map<String, String?>) {
    fun text(name: String): String? = values[name].blankToNull()

    fun long(name: String): Long? = values[name]?.trim()?.toLongOrNull()

    fun int(name: String): Int? {
        val raw = values[name]?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt()
    }

    fun double(name: String): Double? = values[name]?.trim()?.toDoubleOrNull()
}

interface TabularDb {
    fun query(sql: String): List<Row>
}

fun TabularDb.hasTable(name: String): Boolean {
    require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")))
    return query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$name'").isNotEmpty()
}

data class ImportedRoute(
    val sourceKey: String,
    val oldId: Long,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val days: Int,
    val createdAt: String?,
)

data class ImportedSpot(
    val sourceKey: String,
    val oldId: Long,
    val oldRouteId: Long,
    val name: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val dayNumber: Int,
    val orderIndex: Int,
)

data class ImportedPhoto(
    val sourceKey: String,
    val oldId: Long,
    val title: String,
    val description: String?,
    val url: String?,
    val location: String?,
    val oldRouteId: Long?,
    val oldSpotId: Long?,
    val takenAt: String?,
    val createdAt: String?,
)

data class ImportedVideo(
    val sourceKey: String,
    val oldId: Long,
    val title: String,
    val description: String?,
    val url: String?,
    val thumbnailUrl: String?,
    val duration: Int?,
    val createdAt: String?,
)

data class ImportedStory(
    val sourceKey: String,
    val oldId: Long,
    val title: String,
    val content: String,
    val coverUrl: String?,
    val location: String?,
    val photoIds: List<Long>,
    val routeIds: List<Long>,
    val createdAt: String?,
)

fun parseIdList(raw: String?): List<Long> {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty() || text == "null" || text == "[]") return emptyList()
    val body = text.removePrefix("[").removeSuffix("]")
    if (body.isBlank()) return emptyList()
    return body.split(',')
        .mapNotNull { part ->
            val token = part.trim().trim('"')
            if (token.isEmpty()) null else token.toLongOrNull() ?: token.toDoubleOrNull()?.toLong()
        }
        .distinct()
}

fun encodeIdList(ids: List<Long>): String? {
    val unique = ids.distinct().filter { it > 0 }
    if (unique.isEmpty()) return null
    return unique.joinToString(prefix = "[", postfix = "]", separator = ",")
}

fun remapStoredIds(raw: String?, localByOldId: Map<Long, Long>): String? =
    encodeIdList(parseIdList(raw).mapNotNull { localByOldId[it] })

data class WebsiteDump(
    val routes: List<ImportedRoute>,
    val spots: List<ImportedSpot>,
    val photos: List<ImportedPhoto>,
    val videos: List<ImportedVideo>,
    val stories: List<ImportedStory>,
) {
    val isEmpty: Boolean =
        routes.isEmpty() && spots.isEmpty() && photos.isEmpty() && videos.isEmpty() && stories.isEmpty()
}

data class ImportResult(
    val photos: Int,
    val routes: Int,
    val spots: Int,
    val videos: Int,
    val stories: Int,
    val skipped: Int,
    val missingMedia: Int,
) {
    fun message(): String = buildString {
        append("已导入 ")
        append(routes)
        append(" 条路线、")
        append(spots)
        append(" 个景点、")
        append(photos)
        append(" 张照片、")
        append(videos)
        append(" 个视频、")
        append(stories)
        append(" 篇故事。")
        if (skipped > 0) append(" 跳过重复 $skipped 项。")
        if (missingMedia > 0) append(" 有 $missingMedia 个媒体文件没在压缩包里找到，已保留文字内容。")
    }
}

fun readWebsiteDump(db: TabularDb): WebsiteDump {
    val routes = if (db.hasTable("routes")) {
        db.query("SELECT * FROM routes").mapNotNull { it.toRoute() }
    } else {
        emptyList()
    }
    val spots = if (db.hasTable("route_spots")) {
        db.query("SELECT * FROM route_spots").mapNotNull { it.toSpot() }
    } else {
        emptyList()
    }
    val photos = if (db.hasTable("photos")) {
        db.query("SELECT * FROM photos").mapNotNull { it.toPhoto() }
    } else {
        emptyList()
    }
    val videos = if (db.hasTable("videos")) {
        db.query("SELECT * FROM videos").mapNotNull { it.toVideo() }
    } else {
        emptyList()
    }
    val stories = if (db.hasTable("stories")) {
        db.query("SELECT * FROM stories").mapNotNull { it.toStory() }
    } else {
        emptyList()
    }
    return WebsiteDump(routes, spots, photos, videos, stories)
}

fun isSafeZipEntry(name: String): Boolean {
    if (name.isBlank()) return false
    val normalized = name.replace('\\', '/')
    if (normalized.startsWith("/") || normalized.contains(':')) return false
    if (normalized == ".." || normalized.startsWith("../") || normalized.endsWith("/..") || normalized.contains("/../")) {
        return false
    }
    return normalized.split('/').none { it == ".." }
}

fun chooseDatabaseEntry(names: Collection<String>): String? {
    val databases = names.filter { entry ->
        isSafeZipEntry(entry) && entry.replace('\\', '/').substringAfterLast('/') == "travel.db"
    }
    return databases.firstOrNull {
        val normalized = it.replace('\\', '/')
        normalized == "data/travel.db" || normalized.endsWith("/data/travel.db")
    } ?: databases.firstOrNull { it.replace('\\', '/') == "travel.db" }
        ?: databases.minByOrNull { it.length }
}

fun matchZipEntry(entryNames: Collection<String>, rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    val trimmed = rawUrl.trim()
    val lower = trimmed.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")) return null
    val normalized = trimmed.substringBefore('?').substringBefore('#').replace('\\', '/').trimStart('/')
    if (normalized.isBlank() || !isSafeZipEntry(normalized)) return null
    val safeEntries = entryNames.filter { isSafeZipEntry(it) }
    safeEntries.firstOrNull { it.replace('\\', '/') == normalized }?.let { return it }
    val asUploads = when {
        normalized.startsWith("api/uploads/") -> normalized.removePrefix("api/")
        normalized.startsWith("public/uploads/") -> normalized.removePrefix("public/")
        else -> normalized
    }
    safeEntries.firstOrNull { entry ->
        val name = entry.replace('\\', '/')
        name == asUploads ||
            (asUploads.contains('/') && (name.endsWith("/$asUploads") || name == "public/$asUploads"))
    }?.let { return it }
    val uploadsAt = normalized.indexOf("uploads/")
    if (uploadsAt >= 0) {
        val suffix = normalized.substring(uploadsAt)
        safeEntries.firstOrNull { entry ->
            val name = entry.replace('\\', '/')
            name == suffix || name.endsWith("/$suffix") || name == "public/$suffix"
        }?.let { return it }
    }
    return null
}

fun importedRelativePath(kindFolder: String, sourceKey: String, entryName: String): String {
    val base = entryName.replace('\\', '/').substringAfterLast('/')
    val safeBase = base.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_', '.').ifBlank { "file" }
    val safeKey = sourceKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return "media/imported/$kindFolder/${safeKey}_$safeBase"
}

private fun Row.toRoute(): ImportedRoute? {
    val id = long("id") ?: return null
    return ImportedRoute(
        sourceKey = "website:route:$id",
        oldId = id,
        title = text("title") ?: "未命名路线",
        description = text("description"),
        coverUrl = text("cover_url"),
        days = int("days") ?: 1,
        createdAt = text("created_at"),
    )
}

private fun Row.toSpot(): ImportedSpot? {
    val id = long("id") ?: return null
    val routeId = long("route_id") ?: return null
    return ImportedSpot(
        sourceKey = "website:spot:$id",
        oldId = id,
        oldRouteId = routeId,
        name = text("name") ?: "未命名景点",
        description = text("description"),
        latitude = double("latitude"),
        longitude = double("longitude"),
        dayNumber = int("day_number") ?: 1,
        orderIndex = int("order_index") ?: 0,
    )
}

private fun Row.toPhoto(): ImportedPhoto? {
    val id = long("id") ?: return null
    return ImportedPhoto(
        sourceKey = "website:photo:$id",
        oldId = id,
        title = text("title") ?: "未命名照片",
        description = text("description"),
        url = text("url"),
        location = text("location"),
        oldRouteId = long("route_id"),
        oldSpotId = long("spot_id"),
        takenAt = text("taken_at"),
        createdAt = text("created_at"),
    )
}

private fun Row.toVideo(): ImportedVideo? {
    val id = long("id") ?: return null
    return ImportedVideo(
        sourceKey = "website:video:$id",
        oldId = id,
        title = text("title") ?: "未命名视频",
        description = text("description"),
        url = text("url"),
        thumbnailUrl = text("thumbnail_url"),
        duration = int("duration"),
        createdAt = text("created_at"),
    )
}

private fun Row.toStory(): ImportedStory? {
    val id = long("id") ?: return null
    return ImportedStory(
        sourceKey = "website:story:$id",
        oldId = id,
        title = text("title") ?: "未命名故事",
        content = text("content") ?: "",
        coverUrl = text("cover_url"),
        location = text("location"),
        photoIds = parseIdList(text("photo_ids")),
        routeIds = parseIdList(text("route_ids")),
        createdAt = text("created_at"),
    )
}
