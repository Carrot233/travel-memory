package com.carrot233.travelmemory.ui.routes

data class GeoPoint(
    val key: Long,
    val latitude: Double,
    val longitude: Double,
)

data class ProjectedPoint(
    val key: Long,
    val x: Float,
    val y: Float,
)

/**
 * Projects latitude and longitude onto a canvas. North is up. No map tiles or API key.
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
