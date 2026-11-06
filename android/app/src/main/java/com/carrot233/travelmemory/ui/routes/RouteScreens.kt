package com.carrot233.travelmemory.ui.routes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.carrot233.travelmemory.data.SpotDraft
import com.carrot233.travelmemory.data.formatCoord
import com.carrot233.travelmemory.ui.Routes
import com.carrot233.travelmemory.ui.components.ConfirmDeleteDialog
import com.carrot233.travelmemory.ui.components.EmptyHint
import com.carrot233.travelmemory.ui.components.MediaImage
import com.carrot233.travelmemory.ui.components.TravelField
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.editorViewModel
import com.carrot233.travelmemory.ui.edit.RouteEditorViewModel
import com.carrot233.travelmemory.ui.libraryViewModel
import com.carrot233.travelmemory.ui.travelRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(nav: NavHostController) {
    val library = libraryViewModel()
    val routes by library.routes.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(title = "路线")
            if (routes.isEmpty()) {
                EmptyHint(
                    symbol = "🗺️",
                    title = "还没有路线",
                    body = "写下几天的行程，并给景点填上经纬度。地图在本地绘制，不需要密钥。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(routes, key = { it.id }) { route ->
                        Card(onClick = { nav.navigate("route/${route.id}") }) {
                            if (!route.coverUrl.isNullOrBlank()) {
                                MediaImage(
                                    relativePath = route.coverUrl,
                                    description = route.title,
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                )
                            }
                            Column(Modifier.padding(16.dp)) {
                                Text(route.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${route.days} 天",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (!route.description.isNullOrBlank()) {
                                    Text(
                                        route.description,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { nav.navigate(Routes.ROUTE_NEW) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加路线")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(nav: NavHostController, routeId: Long) {
    val repository = travelRepository()
    val details by remember(routeId) { repository.observeRoute(routeId) }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(
            title = details?.route?.title ?: "路线",
            onBack = { nav.popBackStack() },
            actions = {
                if (details != null) {
                    IconButton(onClick = { nav.navigate("route/$routeId/edit") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            },
        )
        val current = details
        if (current == null) {
            EmptyHint("🗺️", "没有找到这条路线", "它可能已经被删除。")
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!current.route.coverUrl.isNullOrBlank()) {
                    Card {
                        MediaImage(
                            relativePath = current.route.coverUrl,
                            description = current.route.title,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    }
                }
                Text(current.route.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${current.route.days} 天行程", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!current.route.description.isNullOrBlank()) {
                    Text(current.route.description)
                }
                Text("路线图", style = MaterialTheme.typography.titleLarge)
                RouteDiagram(current.spots)
                if (current.photos.isNotEmpty()) {
                    Text("这条路线的照片", style = MaterialTheme.typography.titleLarge)
                    current.photos.forEach { photo ->
                        Card(onClick = { nav.navigate("photo/${photo.id}") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MediaImage(
                                    relativePath = photo.url,
                                    description = photo.title,
                                    modifier = Modifier.height(72.dp).fillMaxWidth(0.28f),
                                )
                                Text(photo.title, modifier = Modifier.padding(12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirm && details != null) {
        ConfirmDeleteDialog(
            message = "确定删除这条路线？景点会一起删除，已经关联的照片会保留。",
            onConfirm = {
                confirm = false
                scope.launch {
                    repository.deleteRoute(routeId)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditScreen(nav: NavHostController, routeId: Long?) {
    val editor = editorViewModel(key = "route-$routeId") { RouteEditorViewModel(it, routeId) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingNew by remember { mutableStateOf(false) }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) editor.importCover(context, uri)
    }
    fun leave() {
        editor.abandon()
        nav.popBackStack()
    }
    BackHandler(enabled = !editor.saving) { leave() }
    LaunchedEffect(editor.message) {
        val text = editor.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        editor.message = null
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(
                title = if (routeId == null) "添加路线" else "编辑路线",
                onBack = { if (!editor.saving) leave() },
                actions = {
                    TextButton(enabled = !editor.saving, onClick = { editor.save { nav.popBackStack() } }) {
                        Text(if (editor.saving) "保存中" else "保存")
                    }
                },
            )
            when {
                editor.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                editor.missing -> EmptyHint("🗺️", "没有找到这条路线", "它可能已经被删除。")
                else -> Column(
                    Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!editor.coverPath.isNullOrBlank()) {
                        MediaImage(
                            relativePath = editor.coverPath,
                            description = "路线封面",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                    }
                    Row {
                        TextButton(onClick = {
                            pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("封面") }
                        if (!editor.coverPath.isNullOrBlank()) {
                            TextButton(onClick = { editor.clearCover() }) { Text("移除封面") }
                        }
                    }
                    TravelField("标题", editor.title, { editor.title = it })
                    TravelField("天数", editor.daysText, { editor.daysText = it.filter { ch -> ch.isDigit() }.take(3) }, keyboardType = KeyboardType.Number)
                    TravelField("描述", editor.description, { editor.description = it }, singleLine = false, minLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("景点", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editingNew = true }) { Text("添加") }
                    }
                    if (editor.spots.isEmpty()) {
                        Text("可以只写名字。经纬度可选，用来画本地路线图。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    editor.spots.forEachIndexed { index, spot ->
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(spot.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "第 ${spot.dayNumber} 天" +
                                        if (spot.latitude != null && spot.longitude != null) {
                                            " · ${formatCoord(spot.latitude)}, ${formatCoord(spot.longitude)}"
                                        } else {
                                            ""
                                        },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row {
                                    IconButton(onClick = { editor.moveSpot(index, -1) }) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "上移")
                                    }
                                    IconButton(onClick = { editor.moveSpot(index, 1) }) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "下移")
                                    }
                                    IconButton(onClick = { editingIndex = index }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "编辑景点")
                                    }
                                    IconButton(onClick = { editor.removeSpot(index) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除景点")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    val spot = editingIndex?.let { editor.spots.getOrNull(it) }
    if (editingNew || spot != null) {
        SpotDialog(
            initial = spot,
            onDismiss = {
                editingNew = false
                editingIndex = null
            },
            onSave = { draft ->
                editor.addOrReplaceSpot(if (editingNew) null else editingIndex, draft)
                editingNew = false
                editingIndex = null
            },
            onError = { editor.message = it },
        )
    }
}

@Composable
private fun SpotDialog(
    initial: SpotDraft?,
    onDismiss: () -> Unit,
    onSave: (SpotDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var day by remember { mutableStateOf((initial?.dayNumber ?: 1).toString()) }
    var latitude by remember { mutableStateOf(initial?.latitude?.let { formatCoord(it) }.orEmpty()) }
    var longitude by remember { mutableStateOf(initial?.longitude?.let { formatCoord(it) }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加景点" else "编辑景点") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TravelField("名称", name, { name = it })
                TravelField("第几天", day, { day = it.filter { ch -> ch.isDigit() }.take(3) }, keyboardType = KeyboardType.Number)
                TravelField("纬度", latitude, { latitude = it }, keyboardType = KeyboardType.Decimal)
                TravelField("经度", longitude, { longitude = it }, keyboardType = KeyboardType.Decimal)
                TravelField("描述", description, { description = it }, singleLine = false, minLines = 2)
                Text("经纬度可以留空。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    onError("请填写景点名称")
                    return@TextButton
                }
                val dayNumber = day.toIntOrNull()
                if (dayNumber == null || dayNumber < 1) {
                    onError("请填写第几天")
                    return@TextButton
                }
                val lat = parseCoord(latitude, -90.0, 90.0)
                val lng = parseCoord(longitude, -180.0, 180.0)
                if (lat == CoordParse.Invalid || lng == CoordParse.Invalid) {
                    onError("请输入有效的经纬度，或留空")
                    return@TextButton
                }
                onSave(
                    SpotDraft(
                        id = initial?.id,
                        name = name,
                        description = description,
                        dayNumber = dayNumber,
                        latitude = (lat as? CoordParse.Value)?.number,
                        longitude = (lng as? CoordParse.Value)?.number,
                    ),
                )
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private sealed interface CoordParse {
    data object Empty : CoordParse
    data object Invalid : CoordParse
    data class Value(val number: Double) : CoordParse
}

private fun parseCoord(raw: String, min: Double, max: Double): CoordParse {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return CoordParse.Empty
    val value = trimmed.toDoubleOrNull() ?: return CoordParse.Invalid
    if (value !in min..max) return CoordParse.Invalid
    return CoordParse.Value(value)
}
