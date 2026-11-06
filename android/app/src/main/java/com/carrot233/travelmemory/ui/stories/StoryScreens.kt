package com.carrot233.travelmemory.ui.stories

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.carrot233.travelmemory.ui.components.ConfirmDeleteDialog
import com.carrot233.travelmemory.ui.components.EmptyHint
import com.carrot233.travelmemory.ui.components.MediaImage
import com.carrot233.travelmemory.ui.components.TravelField
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.editorViewModel
import com.carrot233.travelmemory.ui.edit.StoryEditorViewModel
import com.carrot233.travelmemory.ui.libraryViewModel
import com.carrot233.travelmemory.ui.travelRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(nav: NavHostController) {
    val library = libraryViewModel()
    val stories by library.stories.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TravelTopBar(title = "故事")
            if (stories.isEmpty()) {
                EmptyHint(
                    symbol = "📝",
                    title = "还没有故事",
                    body = "把一段旅程写成故事。文字和封面都留在这台设备上。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(stories, key = { it.id }) { story ->
                        Card(onClick = { nav.navigate("story/${story.id}") }) {
                            Column {
                                if (!story.coverUrl.isNullOrBlank()) {
                                    MediaImage(
                                        relativePath = story.coverUrl,
                                        description = story.title,
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                    )
                                }
                                Column(Modifier.padding(16.dp)) {
                                    Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (!story.location.isNullOrBlank()) {
                                        Text(story.location, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        story.content,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 6.dp),
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
            onClick = { nav.navigate(Routes.STORY_NEW) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "写故事")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(nav: NavHostController, storyId: Long) {
    val repository = travelRepository()
    val story by remember(storyId) { repository.observeStory(storyId) }.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(
            title = story?.title ?: "故事",
            onBack = { nav.popBackStack() },
            actions = {
                if (story != null) {
                    IconButton(onClick = { nav.navigate("story/$storyId/edit") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            },
        )
        val current = story
        if (current == null) {
            EmptyHint("📝", "没有找到这篇故事", "它可能已经被删除。")
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!current.coverUrl.isNullOrBlank()) {
                    Card {
                        MediaImage(
                            relativePath = current.coverUrl,
                            description = current.title,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                        )
                    }
                }
                Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val meta = listOfNotNull(
                    current.location?.takeIf { it.isNotBlank() },
                    displayDate(current.createdAt).takeIf { it.isNotBlank() },
                )
                if (meta.isNotEmpty()) {
                    Text(meta.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SelectionContainer {
                    Text(current.content)
                }
            }
        }
    }
    if (confirm && story != null) {
        ConfirmDeleteDialog(
            message = "确定删除这篇故事？",
            onConfirm = {
                val target = story ?: return@ConfirmDeleteDialog
                confirm = false
                scope.launch {
                    repository.deleteStory(target)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditScreen(nav: NavHostController, storyId: Long?) {
    val editor = editorViewModel(key = "story-$storyId") { StoryEditorViewModel(it, storyId) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
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
                title = if (storyId == null) "写故事" else "编辑故事",
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
                editor.missing -> EmptyHint("📝", "没有找到这篇故事", "它可能已经被删除。")
                else -> Column(
                    Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!editor.coverPath.isNullOrBlank()) {
                        MediaImage(
                            relativePath = editor.coverPath,
                            description = "故事封面",
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
                    TravelField("地点", editor.location, { editor.location = it })
                    TravelField("正文", editor.content, { editor.content = it }, singleLine = false, minLines = 8)
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
