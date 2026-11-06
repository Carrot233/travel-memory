package com.carrot233.travelmemory.ui.photos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.carrot233.travelmemory.data.displayDate
import com.carrot233.travelmemory.ui.Routes
import com.carrot233.travelmemory.ui.capture.newCaptureUri
import com.carrot233.travelmemory.ui.capture.rememberPermissionedAction
import com.carrot233.travelmemory.ui.components.ConfirmDeleteDialog
import com.carrot233.travelmemory.ui.components.EmptyHint
import com.carrot233.travelmemory.ui.components.MediaImage
import com.carrot233.travelmemory.ui.components.TravelField
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.editorViewModel
import com.carrot233.travelmemory.ui.edit.PhotoEditorViewModel
import com.carrot233.travelmemory.ui.libraryViewModel
import com.carrot233.travelmemory.ui.shareFile
import com.carrot233.travelmemory.ui.travelRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(nav: NavHostController) {
    val library = libraryViewModel()
    val photos by library.photos.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(title = "照片")
        if (photos.isEmpty()) {
            EmptyHint(
                symbol = "📷",
                title = "还没有照片",
                body = "从相册选一张，或直接拍下来。照片只保存在这台设备上。",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    Card(onClick = { nav.navigate("photo/${photo.id}") }) {
                        MediaImage(
                            relativePath = photo.url,
                            description = photo.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                photo.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (!photo.location.isNullOrBlank()) {
                                Text(
                                    photo.location,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
        FloatingActionButton(
            onClick = { nav.navigate(Routes.PHOTO_NEW) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加照片")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(nav: NavHostController, photoId: Long) {
    val repository = travelRepository()
    val photo by remember(photoId) { repository.observePhoto(photoId) }.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(
            title = photo?.title ?: "照片",
            onBack = { nav.popBackStack() },
            actions = {
                val current = photo
                if (current != null) {
                    IconButton(onClick = {
                        val file = repository.resolveMedia(current.url)
                        if (file != null && file.isFile) shareFile(context, file)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = { nav.navigate("photo/${current.id}/edit") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            },
        )
        val current = photo
        if (current == null) {
            EmptyHint("📷", "没有找到这张照片", "它可能已经被删除。")
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Card {
                    MediaImage(
                        relativePath = current.url,
                        description = current.title,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                    )
                }
                Text(
                    current.title,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                val meta = listOfNotNull(
                    current.location?.takeIf { it.isNotBlank() },
                    displayDate(current.takenAt).takeIf { it.isNotBlank() }?.let { "拍摄于 $it" },
                    displayDate(current.createdAt).takeIf { it.isNotBlank() }?.let { "记录于 $it" },
                )
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString(" · "),
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!current.description.isNullOrBlank()) {
                    Text(current.description, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
    if (confirm && photo != null) {
        ConfirmDeleteDialog(
            message = "确定删除这张照片？设备上的文件也会一并删除。",
            onConfirm = {
                val target = photo ?: return@ConfirmDeleteDialog
                confirm = false
                scope.launch {
                    repository.deletePhoto(target)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(nav: NavHostController, photoId: Long?) {
    val editor = editorViewModel(key = "photo-$photoId") { PhotoEditorViewModel(it, photoId) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val routes by editor.routes.collectAsStateWithLifecycle()
    var showRoutes by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) editor.import(context, uri)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) editor.import(context, uri)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) editor.import(context, uri)
    }
    val requestCamera = rememberPermissionedAction(
        onDenied = { editor.message = "需要相机权限才能拍照" },
        onGranted = {
            val uri = newCaptureUri(context, "jpg")
            cameraUri = uri
            takePicture.launch(uri)
        },
    )
    fun leave() {
        editor.abandon()
        nav.popBackStack()
    }
    BackHandler(enabled = !editor.saving) { leave() }
    LaunchedEffect(editor.message) {
        val text = editor.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        editor.consumeMessage()
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(
                title = if (photoId == null) "添加照片" else "编辑照片",
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
                editor.missing -> EmptyHint("📷", "没有找到这张照片", "它可能已经被删除。")
                else -> Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card {
                        MediaImage(
                            relativePath = editor.imagePath,
                            description = "照片预览",
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 280.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("相册") }
                        TextButton(onClick = { pickFile.launch(arrayOf("image/*")) }) { Text("文件") }
                        TextButton(onClick = requestCamera) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Text("拍照")
                        }
                    }
                    TravelField("标题", editor.title, { editor.title = it })
                    TravelField("地点", editor.location, { editor.location = it })
                    TravelField("拍摄日期", editor.takenAt, { editor.takenAt = it })
                    TravelField("描述", editor.description, { editor.description = it }, singleLine = false, minLines = 3)
                    Text("关联路线", style = MaterialTheme.typography.titleSmall)
                    Text(
                        routes.firstOrNull { it.id == editor.routeId }?.title ?: "不关联路线",
                        modifier = Modifier.clickable { showRoutes = true }.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    if (showRoutes) {
        AlertDialog(
            onDismissRequest = { showRoutes = false },
            title = { Text("关联路线") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    TextButton(onClick = {
                        editor.routeId = null
                        showRoutes = false
                    }) { Text("不关联") }
                    routes.forEach { route ->
                        TextButton(onClick = {
                            editor.routeId = route.id
                            showRoutes = false
                        }) { Text(route.title) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRoutes = false }) { Text("关闭") } },
        )
    }
}
