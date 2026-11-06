package com.carrot233.travelmemory

import com.carrot233.travelmemory.data.MediaFiles
import com.carrot233.travelmemory.data.MediaKind
import com.carrot233.travelmemory.ui.routes.GeoPoint
import com.carrot233.travelmemory.ui.routes.projectSpots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LocalFilesTest {
    @Test
    fun mediaStaysInsideTheAppDirectory() {
        val root = Files.createTempDirectory("travel-memory-media").toFile()
        val media = MediaFiles(root)
        assertNull(media.resolve("../secret"))
        assertNull(media.resolve("media/../../secret"))
        assertNull(media.resolve("/etc/passwd"))
        val relative = media.writeBytes(MediaKind.PHOTO, "jpg", byteArrayOf(1, 2, 3))
        val file = media.resolve(relative)
        assertNotNull(file)
        assertTrue(file!!.readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(file.path.startsWith(root.canonicalFile.path))
        media.deleteOwned(relative)
        assertFalse(file.exists())
        root.deleteRecursively()
    }

    @Test
    fun routeProjectionPutsNorthUp() {
        val projected = projectSpots(
            points = listOf(
                GeoPoint(1, latitude = 30.0, longitude = 120.0),
                GeoPoint(2, latitude = 31.0, longitude = 121.0),
            ),
            width = 200f,
            height = 100f,
            padding = 10f,
        )
        val south = projected.first { it.key == 1L }
        val north = projected.first { it.key == 2L }
        assertTrue(north.y < south.y)
        assertTrue(north.x > south.x)
        assertEquals(10f, south.x, 0.01f)
        assertEquals(190f, north.x, 0.01f)
    }
}
