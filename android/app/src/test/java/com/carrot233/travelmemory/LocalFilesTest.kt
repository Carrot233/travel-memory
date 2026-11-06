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
            markerRadius = 10f,
            lineClearance = 3f,
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
        assertLabelsClearRoute(laidOut, markerRadius = 10f, lineClearance = 3f, labelHeight = 16f)
    }

    private fun assertLabelsClearRoute(
        markers: List<com.carrot233.travelmemory.ui.routes.ProjectedPoint>,
        markerRadius: Float,
        lineClearance: Float,
        labelHeight: Float,
    ) {
        val boxes = markers.map { point ->
            floatArrayOf(point.labelX, point.labelY, point.labelX + point.labelWidth, point.labelY + labelHeight)
        }
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                assertFalse("labels $i and $j overlap", rectanglesOverlap(boxes[i], boxes[j]))
            }
            for (marker in markers) {
                assertFalse(
                    "label $i hits marker ${marker.key}",
                    rectangleHitsCircle(boxes[i], marker.x, marker.y, markerRadius),
                )
            }
        }
        for (index in 0 until markers.lastIndex) {
            val start = markers[index]
            val end = markers[index + 1]
            boxes.forEachIndexed { boxIndex, box ->
                val padded = floatArrayOf(
                    box[0] - lineClearance,
                    box[1] - lineClearance,
                    box[2] + lineClearance,
                    box[3] + lineClearance,
                )
                assertFalse(
                    "label $boxIndex hits the route",
                    segmentHitsRectangle(start.x, start.y, end.x, end.y, padded),
                )
            }
        }
    }

    private fun rectanglesOverlap(a: FloatArray, b: FloatArray): Boolean {
        return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]
    }

    private fun rectangleHitsCircle(rect: FloatArray, cx: Float, cy: Float, radius: Float): Boolean {
        val nearestX = cx.coerceIn(rect[0], rect[2])
        val nearestY = cy.coerceIn(rect[1], rect[3])
        val dx = cx - nearestX
        val dy = cy - nearestY
        return dx * dx + dy * dy < radius * radius
    }

    private fun segmentHitsRectangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        rect: FloatArray,
    ): Boolean {
        if (x1 in rect[0]..rect[2] && y1 in rect[1]..rect[3]) return true
        if (x2 in rect[0]..rect[2] && y2 in rect[1]..rect[3]) return true
        return linesCross(x1, y1, x2, y2, rect[0], rect[1], rect[2], rect[1]) ||
            linesCross(x1, y1, x2, y2, rect[2], rect[1], rect[2], rect[3]) ||
            linesCross(x1, y1, x2, y2, rect[2], rect[3], rect[0], rect[3]) ||
            linesCross(x1, y1, x2, y2, rect[0], rect[3], rect[0], rect[1])
    }

    private fun linesCross(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        dx: Float,
        dy: Float,
    ): Boolean {
        val abx = bx - ax
        val aby = by - ay
        val crossStart = abx * (cy - ay) - aby * (cx - ax)
        val crossEnd = abx * (dy - ay) - aby * (dx - ax)
        val cdx = dx - cx
        val cdy = dy - cy
        val otherStart = cdx * (ay - cy) - cdy * (ax - cx)
        val otherEnd = cdx * (by - cy) - cdy * (bx - cx)
        return crossStart * crossEnd <= 0f && otherStart * otherEnd <= 0f
    }
}
