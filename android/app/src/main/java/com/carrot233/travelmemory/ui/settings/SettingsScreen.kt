package com.carrot233.travelmemory.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.carrot233.travelmemory.data.ThemeMode
import com.carrot233.travelmemory.ui.components.TravelTopBar
import com.carrot233.travelmemory.ui.travelRepository
import com.carrot233.travelmemory.ui.userSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController) {
    val settings = userSettings()
    val repository = travelRepository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
    var importing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            report = null
            report = try {
                repository.importWebsiteZip(context, uri).message()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                error.message ?: "导入失败"
            } finally {
                importing = false
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TravelTopBar(title = "设置", onBack = { nav.popBackStack() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("旅途记忆", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "照片、路线、视频和故事都保存在这台设备上。浏览和新增内容不需要服务器地址，也不会访问网站接口。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("外观", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == ThemeMode.System,
                    onClick = { scope.launch { settings.setThemeMode(ThemeMode.System) } },
                    label = { Text("跟随系统") },
                )
                FilterChip(
                    selected = mode == ThemeMode.Light,
                    onClick = { scope.launch { settings.setThemeMode(ThemeMode.Light) } },
                    label = { Text("浅色") },
                )
                FilterChip(
                    selected = mode == ThemeMode.Dark,
                    onClick = { scope.launch { settings.setThemeMode(ThemeMode.Dark) } },
                    label = { Text("深色") },
                )
            }
            Text("导入网站数据包", style = MaterialTheme.typography.titleMedium)
            Text(
                "可以选择一个 zip，里面有 travel.db，以及 uploads 或 public/uploads 里的文件。这是一次性导入，不会同步，也不会联网。账号和密码不会被导入。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                enabled = !importing,
                onClick = {
                    pickZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                },
            ) {
                Text(if (importing) "正在导入" else "选择 zip 文件")
            }
            if (importing) {
                CircularProgressIndicator()
            }
            if (report != null) {
                Text(report!!)
            }
        }
    }
}
