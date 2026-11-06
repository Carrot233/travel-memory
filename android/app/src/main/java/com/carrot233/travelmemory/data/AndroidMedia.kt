package com.carrot233.travelmemory.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File

fun extensionFor(mime: String?, displayName: String?, kind: MediaKind): String {
    val mapped = when (mime?.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/3gpp" -> "3gp"
        else -> null
    }
    val fromName = displayName?.substringAfterLast('.', "")?.takeIf { it != displayName && it.isNotBlank() }
    val fallback = when (kind) {
        MediaKind.VIDEO -> "mp4"
        else -> "jpg"
    }
    return safeExtension(mapped ?: fromName ?: fallback)
}

fun displayName(resolver: ContentResolver, uri: Uri): String? {
    if (uri.scheme == "file") return uri.lastPathSegment
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0)
    }
    return uri.lastPathSegment
}

fun copyUriIntoStorage(context: Context, media: MediaFiles, uri: Uri, kind: MediaKind): String {
    val resolver = context.contentResolver
    val ext = extensionFor(resolver.getType(uri), displayName(resolver, uri), kind)
    resolver.openInputStream(uri)?.use { input ->
        return media.copyStream(kind, ext, input)
    }
    throw IllegalArgumentException("无法读取所选文件")
}

object VideoMetadataProbe {
    fun read(file: File, media: MediaFiles): Pair<Int?, String?> {
        if (!file.isFile) return null to null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val seconds = durationMs?.let { ((it + 999) / 1000L).toInt() }
            val frame = retriever.getFrameAtTime(0)
            val thumbnail = frame?.let { bitmap ->
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bytes)
                if (!bitmap.isRecycled) bitmap.recycle()
                media.writeBytes(MediaKind.THUMBNAIL, "jpg", bytes.toByteArray())
            }
            seconds to thumbnail
        } catch (error: RuntimeException) {
            null to null
        } finally {
            try {
                retriever.release()
            } catch (error: RuntimeException) {
                // The frame is optional. Playback still works without a thumbnail.
            }
        }
    }
}
