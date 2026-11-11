package com.carrot233.travelmemory.ui.videos

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.carrot233.travelmemory.data.displayDate
import com.carrot233.travelmemory.data.formatDuration
import com.carrot233.travelmemory.ui.Routes
import com.carrot233.travelmemory.ui.capture.newCaptureUri
import com.carrot233.travelmemory.ui.capture.rememberPermissionedAction
import com.carrot233.travelmemory.ui.components.ConfirmDeleteDialog
import com.carrot233.travelmemory.ui.components.EmptyHint
import com.carrot233.travelmemory.ui.components.MediaImage
import com.carrot233.travelmemory.ui.components.TravelField
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.editorViewModel
import com.carrot233.travelmemory.ui.edit.VideoEditorViewModel
import com.carrot233.travelmemory.ui.libraryViewModel
import com.carrot233.travelmemory.ui.shareFile
import com.carrot233.travelmemory.ui.travelRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(nav: NavHostController) {
    val library = libraryViewModel()
    val videos by library.videos.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(title = "视频")
            if (videos.isEmpty()) {
                EmptyHint(
                    symbol = "🎬",
                    title = "还没有视频",
                    body = "从相册选一段旅途视频，或用相机录下来。文件保存在应用私有目录。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(videos, key = { it.id }) { video ->
                        Card(onClick = { nav.navigate("video/${video.id}") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!video.thumbnailUrl.isNullOrBlank()) {
                                    MediaImage(
                                        relativePath = video.thumbnailUrl,
                                        description = video.title,
                                        modifier = Modifier.height(84.dp).aspectRatio(16f / 9f),
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .height(84.dp)
                                            .aspectRatio(16f / 9f)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Videocam, contentDescription = null)
                                    }
                                }
                                Column(Modifier.padding(12.dp).weight(1f)) {
                                    Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                    val duration = formatDuration(video.duration)
                                    if (duration.isNotBlank()) {
                                        Text(duration, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { nav.navigate(Routes.VIDEO_NEW) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加视频")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(nav: NavHostController, videoId: Long) {
    val repository = travelRepository()
    val video by remember(videoId) { repository.observeVideo(videoId) }.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        val current = video
        TravelTopBar(
            title = current?.title ?: "视频",
            onBack = { nav.popBackStack() },
            actions = {
                if (current != null) {
                    IconButton(onClick = {
                        val file = repository.resolveMedia(current.url)
                        if (file != null && file.isFile) shareFile(context, file)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = { nav.navigate("video/${current.id}/edit") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            },
        )
        if (current == null) {
            EmptyHint("🎬", "没有找到这个视频", "它可能已经被删除。")
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val file = repository.resolveMedia(current.url)
                if (file != null && file.isFile) {
                    VideoPlayer(absolutePath = file.absolutePath)
                } else {
                    Text("视频文件不在设备上。", color = MaterialTheme.colorScheme.error)
                }
                Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val meta = listOfNotNull(
                    formatDuration(current.duration).takeIf { it.isNotBlank() },
                    displayDate(current.createdAt).takeIf { it.isNotBlank() },
                )
                if (meta.isNotEmpty()) {
                    Text(meta.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!current.description.isNullOrBlank()) {
                    Text(current.description)
                }
            }
        }
    }
    if (confirm && video != null) {
        ConfirmDeleteDialog(
            message = "确定删除这个视频？设备上的文件也会一并删除。",
            onConfirm = {
                val target = video ?: return@ConfirmDeleteDialog
                confirm = false
                scope.launch {
                    repository.deleteVideo(target)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
fun VideoPlayer(absolutePath: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(java.io.File(absolutePath).toUri()))
            playWhenReady = false
            prepare()
        }
    }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditScreen(nav: NavHostController, videoId: Long?) {
    val editor = editorViewModel(key = "video-$videoId") { VideoEditorViewModel(it, videoId) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) editor.import(context, uri)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) editor.import(context, uri)
    }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) editor.import(context, uri)
    }
    val requestCamera = rememberPermissionedAction(
        onDenied = { editor.message = "需要相机权限才能录像" },
        onGranted = {
            val uri = newCaptureUri(context, "mp4")
            cameraUri = uri
            capture.launch(uri)
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
        editor.message = null
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(
                title = if (videoId == null) "添加视频" else "编辑视频",
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
                editor.missing -> EmptyHint("🎬", "没有找到这个视频", "它可能已经被删除。")
                else -> Column(
                    Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val repository = travelRepository()
                    val path = editor.videoPath?.let { repository.resolveMedia(it) }
                    if (path != null && path.isFile) {
                        VideoPlayer(absolutePath = path.absolutePath)
                    } else {
                        Text("还没有选择视频。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }) { Text("相册") }
                        TextButton(onClick = { pickFile.launch(arrayOf("video/*")) }) { Text("文件") }
                        TextButton(onClick = requestCamera) { Text("录像") }
                    }
                    TravelField("标题", editor.title, { editor.title = it })
                    TravelField("描述", editor.description, { editor.description = it }, singleLine = false, minLines = 3)
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
