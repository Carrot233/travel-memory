package com.carrot233.travelmemory

import com.carrot233.travelmemory.data.Row
import com.carrot233.travelmemory.data.TabularDb
import com.carrot233.travelmemory.data.chooseDatabaseEntry
import com.carrot233.travelmemory.data.importedRelativePath
import com.carrot233.travelmemory.data.isSafeZipEntry
import com.carrot233.travelmemory.data.matchZipEntry
import com.carrot233.travelmemory.data.readWebsiteDump
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WebsiteArchiveTest {
    @Test
    fun readsWebsiteTablesAndIgnoresAccounts() {
        val queried = mutableListOf<String>()
        val db = object : TabularDb {
            override fun query(sql: String): List<Row> {
                queried += sql
                val compact = sql.trim().replace(Regex("\\s+"), " ")
                if (compact.startsWith("SELECT name FROM sqlite_master")) {
                    val name = Regex("name = '([A-Za-z0-9_]+)'").find(compact)?.groupValues?.get(1)
                    val known = setOf("photos", "routes", "route_spots", "videos", "stories")
                    return if (name in known) listOf(Row(mapOf("name" to name))) else emptyList()
                }
                val table = Regex("FROM ([A-Za-z0-9_]+)").find(compact)?.groupValues?.get(1)
                return when (table) {
                    "routes" -> listOf(
                        Row(mapOf("id" to "3", "title" to "杭州", "days" to "2", "cover_url" to "/api/uploads/photos/cover.jpg")),
                    )
                    "route_spots" -> listOf(
                        Row(
                            mapOf(
                                "id" to "9",
                                "route_id" to "3",
                                "name" to "西湖",
                                "latitude" to "30.25",
                                "longitude" to "120.14",
                                "day_number" to "1",
                                "order_index" to "0",
                            ),
                        ),
                    )
                    "photos" -> listOf(
                        Row(mapOf("id" to "4", "title" to "日落", "url" to "public/uploads/photos/a.jpg", "route_id" to "3")),
                    )
                    "videos" -> listOf(
                        Row(mapOf("id" to "5", "title" to "游船", "url" to "https://example.com/video.mp4")),
                    )
                    "stories" -> listOf(
                        Row(mapOf("id" to "6", "title" to "雨", "content" to "下雨了")),
                    )
                    else -> emptyList()
                }
            }
        }
        val dump = readWebsiteDump(db)
        assertEquals("杭州", dump.routes.single().title)
        assertEquals(2, dump.routes.single().days)
        assertEquals("西湖", dump.spots.single().name)
        assertEquals(30.25, dump.spots.single().latitude!!, 0.001)
        assertEquals("日落", dump.photos.single().title)
        assertEquals(3L, dump.photos.single().oldRouteId)
        assertEquals("游船", dump.videos.single().title)
        assertEquals("下雨了", dump.stories.single().content)
        assertTrue(queried.none { it.contains("users") || it.contains("password") })
        assertNull(matchZipEntry(listOf("public/uploads/photos/a.jpg"), dump.videos.single().url))
    }

    @Test
    fun matchesUploadPathsAndRejectsTraversal() {
        val entries = listOf(
            "data/travel.db",
            "public/uploads/photos/a.jpg",
            "uploads/videos/b.mp4",
            "../secret.txt",
        )
        assertEquals("public/uploads/photos/a.jpg", matchZipEntry(entries, "/api/uploads/photos/a.jpg"))
        assertEquals("uploads/videos/b.mp4", matchZipEntry(entries, "uploads/videos/b.mp4"))
        assertNull(matchZipEntry(entries, "http://10.0.2.2:3001/api/uploads/photos/a.jpg"))
        assertNull(matchZipEntry(listOf("one/a.jpg", "two/a.jpg"), "a.jpg"))
        assertFalse(isSafeZipEntry("../secret.txt"))
        assertFalse(isSafeZipEntry("/etc/passwd"))
        assertFalse(isSafeZipEntry("uploads/../../secret"))
        assertEquals("data/travel.db", chooseDatabaseEntry(entries))
        assertTrue(importedRelativePath("photos", "website:photo:4", "public/uploads/photos/a.jpg").endsWith("_a.jpg"))
    }

    @Test
    fun sourcesDoNotReferenceABackend() {
        val root = listOf(File("src/main"), File("app/src/main"), File("android/app/src/main"))
            .firstOrNull { File(it, "AndroidManifest.xml").isFile }
            ?: error("找不到 AndroidManifest.xml，cwd=${File(".").absolutePath}")
        val forbidden = listOf("10.0.2.2", "http://localhost", "127.0.0.1", ":3001", "retrofit2", "Retrofit")
        root.walk().filter { it.extension == "kt" || it.extension == "xml" }.forEach { file ->
            val text = file.readText()
            forbidden.forEach { token ->
                assertFalse("${file.path} contains $token", text.contains(token))
            }
        }
        val manifest = File(root, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("tools:node=\"remove\""))
        assertTrue(manifest.contains("旅途记忆") || File(root, "res/values/strings.xml").readText().contains("旅途记忆"))
    }
}
