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
    val labelWidth: Float = 72f,
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
    markerRadius: Float = 8f,
    lineClearance: Float = 4f,
): List<ProjectedPoint> {
    val projected = projectSpots(points, width, height, padding)
    val separated = if (projected.size < 2 || minSeparation <= 0f) {
        projected
    } else {
        separateProjected(
            points = projected,
            minDistance = minSeparation,
            minX = padding,
            minY = padding,
            maxX = width - padding,
            maxY = height - padding,
        )
    }
    return placeLabels(
        points = separated,
        labelWidths = labelWidths,
        labelHeight = labelHeight,
        minX = 4f,
        minY = 2f,
        maxX = width - 4f,
        maxY = height - 4f,
        markerRadius = markerRadius,
        lineClearance = lineClearance,
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
    markerRadius: Float,
    lineClearance: Float,
): List<ProjectedPoint> {
    val placed = ArrayList<FloatArray>(points.size)
    val height = labelHeight.coerceAtLeast(12f)
    return points.mapIndexed { index, point ->
        val fullWidth = labelWidths.getOrElse(index) { 72f }.coerceIn(28f, (maxX - minX).coerceAtLeast(28f))
        val widths = listOf(fullWidth, fullWidth * 0.68f, 32f.coerceAtMost(fullWidth)).distinct()
        var chosenWidth = widths.last()
        var chosen = widths.firstNotNullOfOrNull { width ->
            findClearLabel(point, points, placed, width, height, minX, minY, maxX, maxY, markerRadius, lineClearance)
                ?.also { chosenWidth = width }
        }
        if (chosen == null) {
            chosen = nudgeLabel(
                point, points, placed, chosenWidth, height, minX, minY, maxX, maxY, markerRadius, lineClearance,
            )
        }
        val box = chosen ?: floatArrayOf(
            point.x + markerRadius + 6f,
            point.y - height / 2f,
            point.x + markerRadius + 6f + chosenWidth,
            point.y + height / 2f,
        )
        placed += box
        point.copy(labelX = box[0], labelY = box[1], labelWidth = box[2] - box[0])
    }
}

private val labelDirections = arrayOf(
    1f to 0f,
    -1f to 0f,
    0f to -1f,
    0f to 1f,
    0.7071f to -0.7071f,
    -0.7071f to -0.7071f,
    0.7071f to 0.7071f,
    -0.7071f to 0.7071f,
)

private fun findClearLabel(
    point: ProjectedPoint,
    markers: List<ProjectedPoint>,
    placed: List<FloatArray>,
    width: Float,
    height: Float,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    markerRadius: Float,
    lineClearance: Float,
): FloatArray? {
    val gap = 6f
    for (extra in floatArrayOf(0f, height, height * 2f)) {
        for ((dx, dy) in labelDirections) {
            val reach = markerRadius + gap + extra + kotlin.math.abs(dx) * width / 2f + kotlin.math.abs(dy) * height / 2f
            val left = point.x + dx * reach - width / 2f
            val top = point.y + dy * reach - height / 2f
            val box = floatArrayOf(left, top, left + width, top + height)
            if (labelIsClear(box, markers, placed, minX, minY, maxX, maxY, markerRadius, lineClearance)) {
                return box
            }
        }
    }
    return null
}

private fun nudgeLabel(
    point: ProjectedPoint,
    markers: List<ProjectedPoint>,
    placed: List<FloatArray>,
    width: Float,
    height: Float,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    markerRadius: Float,
    lineClearance: Float,
): FloatArray? {
    var left = (point.x - width / 2f).coerceIn(minX, (maxX - width).coerceAtLeast(minX))
    var top = (point.y - markerRadius - 6f - height).coerceIn(minY, (maxY - height).coerceAtLeast(minY))
    repeat(12) {
        val box = floatArrayOf(left, top, left + width, top + height)
        if (labelIsClear(box, markers, placed, minX, minY, maxX, maxY, markerRadius, lineClearance)) {
            return box
        }
        var pushX = 0f
        var pushY = 0f
        for (marker in markers) {
            val nearestX = marker.x.coerceIn(box[0], box[2])
            val nearestY = marker.y.coerceIn(box[1], box[3])
            val dx = nearestX - marker.x
            val dy = nearestY - marker.y
            val distance = hypot(dx, dy).coerceAtLeast(0.5f)
            if (distance < markerRadius + 4f) {
                pushX += dx / distance
                pushY += dy / distance
            }
        }
        if (pushX == 0f && pushY == 0f) {
            pushY = -1f
        }
        left = (left + pushX * 8f).coerceIn(minX, (maxX - width).coerceAtLeast(minX))
        top = (top + pushY * 8f).coerceIn(minY, (maxY - height).coerceAtLeast(minY))
    }
    return null
}

private fun labelIsClear(
    box: FloatArray,
    markers: List<ProjectedPoint>,
    placed: List<FloatArray>,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    markerRadius: Float,
    lineClearance: Float,
): Boolean {
    if (box[0] < minX || box[1] < minY || box[2] > maxX || box[3] > maxY) return false
    val padded = floatArrayOf(
        box[0] - 2f,
        box[1] - 2f,
        box[2] + 2f,
        box[3] + 2f,
    )
    if (placed.any { overlaps(it, padded) }) return false
    if (markers.any { rectHitsCircle(padded, it.x, it.y, markerRadius) }) return false
    val lineBox = floatArrayOf(
        box[0] - lineClearance,
        box[1] - lineClearance,
        box[2] + lineClearance,
        box[3] + lineClearance,
    )
    for (index in 0 until markers.lastIndex) {
        val start = markers[index]
        val end = markers[index + 1]
        if (segmentHitsRect(start.x, start.y, end.x, end.y, lineBox)) return false
    }
    return true
}

private fun rectHitsCircle(rect: FloatArray, cx: Float, cy: Float, radius: Float): Boolean {
    val nearestX = cx.coerceIn(rect[0], rect[2])
    val nearestY = cy.coerceIn(rect[1], rect[3])
    val dx = cx - nearestX
    val dy = cy - nearestY
    return dx * dx + dy * dy < radius * radius
}

private fun segmentHitsRect(x1: Float, y1: Float, x2: Float, y2: Float, rect: FloatArray): Boolean {
    if (pointInRect(x1, y1, rect) || pointInRect(x2, y2, rect)) return true
    val left = rect[0]
    val top = rect[1]
    val right = rect[2]
    val bottom = rect[3]
    return segmentsCross(x1, y1, x2, y2, left, top, right, top) ||
        segmentsCross(x1, y1, x2, y2, right, top, right, bottom) ||
        segmentsCross(x1, y1, x2, y2, right, bottom, left, bottom) ||
        segmentsCross(x1, y1, x2, y2, left, bottom, left, top)
}

private fun pointInRect(x: Float, y: Float, rect: FloatArray): Boolean {
    return x >= rect[0] && x <= rect[2] && y >= rect[1] && y <= rect[3]
}

private fun segmentsCross(
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
    val acx = cx - ax
    val acy = cy - ay
    val adx = dx - ax
    val ady = dy - ay
    val cdx = dx - cx
    val cdy = dy - cy
    val cax = ax - cx
    val cay = ay - cy
    val cbx = bx - cx
    val cby = by - cy
    val abAcross = abx * acy - aby * acx
    val abAcrossEnd = abx * ady - aby * adx
    val cdAcross = cdx * cay - cdy * cax
    val cdAcrossEnd = cdx * cby - cdy * cbx
    return abAcross * abAcrossEnd <= 0f && cdAcross * cdAcrossEnd <= 0f
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
