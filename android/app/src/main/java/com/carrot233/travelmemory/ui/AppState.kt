package com.carrot233.travelmemory.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carrot233.travelmemory.data.TravelRepository
import com.carrot233.travelmemory.data.UserSettings
import com.carrot233.travelmemory.data.mimeForPath
import com.carrot233.travelmemory.travelApp
import java.io.File

object Routes {
    const val HOME = "home"
    const val PHOTOS = "photos"
    const val PHOTO_NEW = "photo/new"
    const val PHOTO_EDIT = "photo/{id}/edit"
    const val PHOTO = "photo/{id}"
    const val ROUTES = "routes"
    const val ROUTE_NEW = "route/new"
    const val ROUTE_EDIT = "route/{id}/edit"
    const val ROUTE = "route/{id}"
    const val VIDEOS = "videos"
    const val VIDEO_NEW = "video/new"
    const val VIDEO_EDIT = "video/{id}/edit"
    const val VIDEO = "video/{id}"
    const val STORIES = "stories"
    const val STORY_NEW = "story/new"
    const val STORY_EDIT = "story/{id}/edit"
    const val STORY = "story/{id}"
    const val SETTINGS = "settings"

    val topLevel = setOf(HOME, PHOTOS, ROUTES, VIDEOS, STORIES)
}

@Composable
fun travelRepository(): TravelRepository = LocalContext.current.travelApp().container.repository

@Composable
fun userSettings(): UserSettings = LocalContext.current.travelApp().container.settings

fun <VM : ViewModel> simpleFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

@Composable
inline fun <reified VM : ViewModel> editorViewModel(key: String, noinline create: (TravelRepository) -> VM): VM {
    val repository = travelRepository()
    return viewModel(key = key, factory = simpleFactory { create(repository) })
}

@Composable
fun libraryViewModel(): LibraryViewModel {
    val activity = LocalContext.current as ComponentActivity
    val repository = travelRepository()
    return viewModel(
        viewModelStoreOwner = activity,
        factory = simpleFactory { LibraryViewModel(repository) },
    )
}

fun shareFile(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeForPath(file.name)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
