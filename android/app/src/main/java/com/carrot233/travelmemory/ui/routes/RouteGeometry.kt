package com.carrot233.travelmemory.ui.routes

import kotlin.math.hypot

data class GeoPoint(
    val key: Long,
    val latitude: Double,
    val longitude: Double,
)

data class ProjectedPoint(
    val key: Long,
    val x: Float,
    val y: Float,
    val labelX: Float = x + 14f,
    val labelY: Float = (y - 28f).coerceAtLeast(0f),
)

/**
 * Projects latitude and longitude onto a canvas. North is up. No map tiles or API key.
 * This is the geographic fit only: a long route still collapses nearby spots.
 */
fun projectSpots(
    points: List<GeoPoint>,
    width: Float,
    height: Float,
    padding: Float,
): List<ProjectedPoint> {
    if (points.isEmpty() || width <= padding * 2 || height <= padding * 2) return emptyList()
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }
    val latSpan = (maxLat - minLat).takeIf { it > 1e-8 }
    val lngSpan = (maxLng - minLng).takeIf { it > 1e-8 }
    val drawWidth = width - padding * 2
    val drawHeight = height - padding * 2
    return points.map { point ->
        val x = if (lngSpan == null) {
            width / 2f
        } else {
            padding + ((point.longitude - minLng) / lngSpan).toFloat() * drawWidth
        }
        val y = if (latSpan == null) {
            height / 2f
        } else {
            padding + ((maxLat - point.latitude) / latSpan).toFloat() * drawHeight
        }
        ProjectedPoint(point.key, x, y)
    }
}

/**
 * Geographic projection, then a screen-space nudge so markers that landed on the
 * same pixels are pulled apart. Distant anchors stay put, so the whole route
 * remains visible.
 */
fun layoutRouteMarkers(
    points: List<GeoPoint>,
    labelWidths: List<Float>,
    labelHeight: Float,
    width: Float,
    height: Float,
    padding: Float,
    minSeparation: Float,
): List<ProjectedPoint> {
    val projected = projectSpots(points, width, height, padding)
    if (projected.size < 2 || minSeparation <= 0f) return projected
    val separated = separateProjected(
        points = projected,
        minDistance = minSeparation,
        minX = padding,
        minY = padding,
        maxX = width - padding,
        maxY = height - padding,
    )
    return placeLabels(
        points = separated,
        labelWidths = labelWidths,
        labelHeight = labelHeight,
        minX = 4f,
        minY = 2f,
        maxX = width - 4f,
        maxY = height - 4f,
    )
}

fun separateProjected(
    points: List<ProjectedPoint>,
    minDistance: Float,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
): List<ProjectedPoint> {
    if (points.size < 2 || minDistance <= 0f || maxX <= minX || maxY <= minY) return points
    val xs = FloatArray(points.size) { points[it].x.coerceIn(minX, maxX) }
    val ys = FloatArray(points.size) { points[it].y.coerceIn(minY, maxY) }
    val anchored = BooleanArray(points.size) { index ->
        nearestDistance(xs, ys, index) >= minDistance
    }
    val originX = xs.copyOf()
    val originY = ys.copyOf()
    var step = 0
    while (step < 48) {
        var moved = false
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                if (anchored[i] && anchored[j]) continue
                var dx = xs[j] - xs[i]
                var dy = ys[j] - ys[i]
                var distance = hypot(dx, dy)
                if (distance >= minDistance) continue
                if (distance < 0.8f) {
                    val inwardX = ((minX + maxX) / 2f) - ((originX[i] + originX[j]) / 2f)
                    val inwardY = ((minY + maxY) / 2f) - ((originY[i] + originY[j]) / 2f)
                    val inward = hypot(inwardX, inwardY)
                    if (inward < 1f) {
                        dx = 1f
                        dy = 0f
                    } else {
                        dx = -inwardY / inward
                        dy = inwardX / inward
                    }
                    distance = 1f
                }
                val push = (minDistance - distance.coerceAtMost(minDistance)) / 
                    (if (anchored[i] || anchored[j]) 1f else 2f)
                val ux = dx / distance
                val uy = dy / distance
                if (!anchored[i]) {
                    xs[i] -= ux * push
                    ys[i] -= uy * push
                }
                if (!anchored[j]) {
                    xs[j] += ux * push
                    ys[j] += uy * push
                }
                moved = true
            }
        }
        for (index in points.indices) {
            xs[index] = xs[index].coerceIn(minX, maxX)
            ys[index] = ys[index].coerceIn(minY, maxY)
        }
        if (!moved) break
        step += 1
    }
    return points.mapIndexed { index, point ->
        point.copy(x = xs[index], y = ys[index])
    }
}

private fun placeLabels(
    points: List<ProjectedPoint>,
    labelWidths: List<Float>,
    labelHeight: Float,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
): List<ProjectedPoint> {
    val placed = ArrayList<FloatArray>(points.size)
    return points.mapIndexed { index, point ->
        val width = labelWidths.getOrElse(index) { 72f }.coerceAtLeast(24f)
        val height = labelHeight.coerceAtLeast(12f)
        val gap = 8f
        val candidates = listOf(
            point.x + gap to point.y - height,
            point.x + gap to point.y + gap,
            point.x - width - gap to point.y - height,
            point.x - width - gap to point.y + gap,
            point.x - width / 2f to point.y - height - gap,
            point.x - width / 2f to point.y + gap + 10f,
        )
        val chosen = candidates.firstOrNull { (left, top) ->
            val box = floatArrayOf(left, top, left + width, top + height)
            box[0] >= minX && box[1] >= minY && box[2] <= maxX && box[3] <= maxY &&
                placed.none { overlaps(it, box) }
        } ?: candidates.first()
        val box = floatArrayOf(chosen.first, chosen.second, chosen.first + width, chosen.second + height)
        placed += box
        point.copy(labelX = chosen.first.coerceIn(minX, maxX), labelY = chosen.second.coerceIn(minY, maxY))
    }
}

private fun nearestDistance(xs: FloatArray, ys: FloatArray, index: Int): Float {
    var nearest = Float.MAX_VALUE
    for (other in xs.indices) {
        if (other == index) continue
        val distance = hypot(xs[other] - xs[index], ys[other] - ys[index])
        if (distance < nearest) nearest = distance
    }
    return nearest
}

private fun overlaps(a: FloatArray, b: FloatArray): Boolean {
    return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]
}
