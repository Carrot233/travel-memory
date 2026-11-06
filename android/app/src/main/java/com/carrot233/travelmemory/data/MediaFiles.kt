package com.carrot233.travelmemory.data

import java.io.File
import java.io.InputStream
import java.util.UUID

enum class MediaKind(val folder: String) {
    PHOTO("photos"),
    VIDEO("videos"),
    THUMBNAIL("thumbnails"),
    COVER("covers"),
}

/**
 * App-private files under [root]/media. Paths stored in Room are relative to [root].
 */
class MediaFiles(private val root: File) {
    fun writeBytes(kind: MediaKind, extension: String, bytes: ByteArray): String {
        val relative = newRelative(kind, extension)
        val file = requireResolved(relative)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return relative
    }

    fun copyStream(kind: MediaKind, extension: String, input: InputStream): String {
        val relative = newRelative(kind, extension)
        val file = requireResolved(relative)
        file.parentFile?.mkdirs()
        file.outputStream().use { output -> input.copyTo(output) }
        return relative
    }

    fun writeToRelative(relative: String, input: InputStream) {
        val file = requireResolved(relative)
        file.parentFile?.mkdirs()
        file.outputStream().use { output -> input.copyTo(output) }
    }

    fun resolve(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val normalized = relativePath.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains(':')) return null
        val parts = normalized.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null
        val rootCanon = root.canonicalFile
        val file = File(rootCanon, parts.joinToString(File.separator)).canonicalFile
        val rootPath = rootCanon.path
        if (file.path != rootPath && !file.path.startsWith(rootPath + File.separator)) return null
        return file
    }

    fun deleteOwned(relativePath: String?) {
        val file = resolve(relativePath) ?: return
        val mediaRoot = File(root, "media").canonicalFile
        if (!file.path.startsWith(mediaRoot.path + File.separator)) return
        if (file.isFile) file.delete()
    }

    private fun newRelative(kind: MediaKind, extension: String): String {
        val ext = safeExtension(extension)
        val name = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
        return "media/${kind.folder}/$name"
    }

    private fun requireResolved(relative: String): File =
        resolve(relative) ?: error("非法的媒体路径")
}

fun safeExtension(raw: String?): String {
    val ext = raw?.lowercase()?.filter { it.isLetterOrDigit() }.orEmpty()
    return if (ext.length in 1..5) ext else "bin"
}
