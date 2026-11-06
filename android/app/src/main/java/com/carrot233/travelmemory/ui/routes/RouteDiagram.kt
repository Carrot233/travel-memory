package com.carrot233.travelmemory.ui.routes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.carrot233.travelmemory.data.RouteSpotEntity
import com.carrot233.travelmemory.data.formatCoord

private val dayColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF14B8A6),
    Color(0xFF22C55E),
    Color(0xFFF59E0B),
    Color(0xFFA855F7),
    Color(0xFFEF4444),
)

fun dayColor(day: Int): Color = dayColors[(day - 1).coerceAtLeast(0) % dayColors.size]

@Composable
fun RouteDiagram(spots: List<RouteSpotEntity>, modifier: Modifier = Modifier) {
    if (spots.isEmpty()) {
        Text(
            "还没有景点。编辑路线时可以按天添加停留点。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val days = spots.map { it.dayNumber }.distinct().sorted()
    var selectedDay by remember { mutableIntStateOf(0) }
    val shown = if (selectedDay == 0) spots else spots.filter { it.dayNumber == selectedDay }
    val ordered = shown.sortedWith(compareBy({ it.dayNumber }, { it.orderIndex }, { it.id }))
    val located = ordered.filter { it.latitude != null && it.longitude != null }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(selected = selectedDay == 0, onClick = { selectedDay = 0 }, label = { Text("全部") })
            days.forEach { day ->
                FilterChip(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    label = { Text("第 $day 天") },
                )
            }
        }
        if (located.isNotEmpty()) {
            CoordMap(located)
            Text(
                "本地示意图，不需要地图密钥。靠得很近的点会分开画，整条路线仍然都在图里。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ordered.forEachIndexed { index, spot ->
            SpotRow(spot = spot, showRail = index != ordered.lastIndex)
        }
    }
}

@Composable
private fun CoordMap(spots: List<RouteSpotEntity>) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val markerRadius = with(density) { 8.dp.toPx() }
    val minSeparation = with(density) { 36.dp.toPx() }
    val labelHeight = with(density) { 16.dp.toPx() }
    val projected = if (size.width == 0 || size.height == 0) {
        emptyList()
    } else {
        layoutRouteMarkers(
            points = spots.map { GeoPoint(it.id, it.latitude ?: 0.0, it.longitude ?: 0.0) },
            labelWidths = spots.map { spot ->
                with(density) { (spot.name.length * 14).dp.toPx().coerceAtMost(108.dp.toPx()) }
            },
            labelHeight = labelHeight,
            width = size.width.toFloat(),
            height = size.height.toFloat(),
            padding = with(density) { 40.dp.toPx() },
            minSeparation = minSeparation,
            markerRadius = markerRadius,
            lineClearance = with(density) { 4.dp.toPx() },
        )
    }
    val lookup = spots.associateBy { it.id }
    Surface(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (projected.size >= 2) {
                    val path = Path().apply {
                        moveTo(projected.first().x, projected.first().y)
                        projected.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path,
                        color = Color(0xFF1D6FE8),
                        style = Stroke(width = with(density) { 2.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                projected.forEach { point ->
                    val spot = lookup[point.key] ?: return@forEach
                    drawCircle(dayColor(spot.dayNumber), radius = markerRadius, center = Offset(point.x, point.y))
                    drawCircle(Color.White, radius = markerRadius * 0.38f, center = Offset(point.x, point.y))
                }
            }
            projected.forEach { point ->
                val spot = lookup[point.key] ?: return@forEach
                Text(
                    spot.name,
                    modifier = Modifier
                        .offset { IntOffset(point.labelX.roundToInt(), point.labelY.roundToInt()) }
                        .width(with(density) { point.labelWidth.toDp() })
                        .height(with(density) { labelHeight.toDp() })
                        .clipToBounds(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun SpotRow(spot: RouteSpotEntity, showRail: Boolean) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(12.dp)
                    .background(dayColor(spot.dayNumber), CircleShape),
            )
            if (showRail) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = 8.dp)) {
            Text(spot.name, style = MaterialTheme.typography.titleMedium)
            val detail = buildList {
                add("第 ${spot.dayNumber} 天")
                if (spot.latitude != null && spot.longitude != null) {
                    add("${formatCoord(spot.latitude)}, ${formatCoord(spot.longitude)}")
                }
            }.joinToString(" · ")
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!spot.description.isNullOrBlank()) {
                Text(spot.description, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
