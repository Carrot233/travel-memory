package com.carrot233.travelmemory.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.carrot233.travelmemory.ui.Routes
import com.carrot233.travelmemory.ui.photoSharedBounds
import com.carrot233.travelmemory.ui.components.MediaImage
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.libraryViewModel
import com.carrot233.travelmemory.ui.openSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val library = libraryViewModel()
    val counts by library.counts.collectAsStateWithLifecycle()
    val photos by library.photos.collectAsStateWithLifecycle()
    val routes by library.routes.collectAsStateWithLifecycle()
    val stories by library.stories.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(
            title = "旅途记忆",
            actions = {
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
            },
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF14B8A6), Color(0xFF22C55E)),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            ) {
                Text(
                    "旅途记忆",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "记录每一段美好旅程，分享旅途中的点点滴滴",
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { nav.navigate(Routes.PHOTO_NEW) }) { Text("记一张照片") }
                    OutlinedButton(onClick = { nav.navigate(Routes.STORY_NEW) }) {
                        Text("写一篇故事", color = Color.White)
                    }
                }
            }
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.padding(vertical = 16.dp)) {
                    Stat("照片", counts.photos, Color(0xFF3B82F6), Modifier.weight(1f)) { nav.openSection(Routes.PHOTOS) }
                    Stat("路线", counts.routes, Color(0xFF14B8A6), Modifier.weight(1f)) { nav.openSection(Routes.ROUTES) }
                    Stat("视频", counts.videos, Color(0xFF22C55E), Modifier.weight(1f)) { nav.openSection(Routes.VIDEOS) }
                    Stat("故事", counts.stories, Color(0xFFA855F7), Modifier.weight(1f)) { nav.openSection(Routes.STORIES) }
                }
            }
            if (counts.photos + counts.routes + counts.videos + counts.stories == 0) {
                Text(
                    "从这里开始记下一段旅程。照片、路线、视频和故事都只会留在这台设备上。",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionHeader("最新照片", "查看全部") { nav.openSection(Routes.PHOTOS) }
            if (photos.isEmpty()) {
                Text("暂无照片", modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(photos.take(8), key = { it.id }) { photo ->
                        Card(
                            onClick = { nav.navigate("photo/${photo.id}") },
                            modifier = Modifier.width(160.dp),
                        ) {
                            MediaImage(
                                relativePath = photo.url,
                                description = photo.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .photoSharedBounds(photo.id),
                            )
                            Column(Modifier.padding(10.dp)) {
                                Text(photo.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
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
            Spacer(Modifier.height(8.dp))
            SectionHeader("最近路线", "查看全部") { nav.openSection(Routes.ROUTES) }
            if (routes.isEmpty()) {
                Text("暂无路线", modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    routes.take(3).forEach { route ->
                        Card(onClick = { nav.navigate("route/${route.id}") }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(route.title, style = MaterialTheme.typography.titleMedium)
                                Text("${route.days} 天", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SectionHeader("最近故事", "查看全部") { nav.openSection(Routes.STORIES) }
            if (stories.isEmpty()) {
                Text("暂无故事", modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stories.take(3).forEach { story ->
                        Card(onClick = { nav.navigate("story/${story.id}") }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    story.content,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, count: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            count.toString(),
            color = color,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
        androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
    }
}
