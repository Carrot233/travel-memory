package com.carrot233.travelmemory

import com.carrot233.travelmemory.data.MediaFiles
import com.carrot233.travelmemory.data.MediaKind
import com.carrot233.travelmemory.ui.routes.GeoPoint
import com.carrot233.travelmemory.ui.routes.layoutRouteMarkers
import com.carrot233.travelmemory.ui.routes.projectSpots
import kotlin.math.hypot
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

    @Test
    fun nearbySpotsSeparateWhenTheRouteAlsoHasADistantPoint() {
        val laidOut = layoutRouteMarkers(
            points = listOf(
                GeoPoint(1, latitude = 31.2304, longitude = 121.4737),
                GeoPoint(2, latitude = 31.2305, longitude = 121.4738),
                GeoPoint(3, latitude = 39.9042, longitude = 116.4074),
            ),
            labelWidths = listOf(72f, 72f, 72f),
            labelHeight = 16f,
            width = 800f,
            height = 500f,
            padding = 40f,
            minSeparation = 40f,
        )
        val nearA = laidOut.first { it.key == 1L }
        val nearB = laidOut.first { it.key == 2L }
        val far = laidOut.first { it.key == 3L }
        val closeGap = hypot(nearA.x - nearB.x, nearA.y - nearB.y)
        val farSpan = hypot(nearA.x - far.x, nearA.y - far.y)
        assertTrue(closeGap >= 39f)
        assertTrue(farSpan > closeGap * 4)
        assertTrue(far.x < nearA.x && far.x < nearB.x)
        assertTrue(far.y < nearA.y && far.y < nearB.y)
        laidOut.forEach { point ->
            assertTrue(point.x in 40f..760f)
            assertTrue(point.y in 40f..460f)
        }
        val labelGap = hypot(nearA.labelX - nearB.labelX, nearA.labelY - nearB.labelY)
        assertTrue(labelGap >= 8f)
    }
}
