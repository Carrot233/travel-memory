package com.carrot233.travelmemory.ui.edit

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carrot233.travelmemory.data.MediaKind
import com.carrot233.travelmemory.data.PhotoDraft
import com.carrot233.travelmemory.data.parseIdList
import com.carrot233.travelmemory.data.RouteDraft
import com.carrot233.travelmemory.data.SpotDraft
import com.carrot233.travelmemory.data.StoryDraft
import com.carrot233.travelmemory.data.TravelRepository
import com.carrot233.travelmemory.data.VideoDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoEditorViewModel(
    private val repository: TravelRepository,
    private val photoId: Long?,
) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf("")
    var takenAt by mutableStateOf("")
    var routeId by mutableStateOf<Long?>(null)
    var imagePath by mutableStateOf<String?>(null)
    var originalPath by mutableStateOf<String?>(null)
    var loading by mutableStateOf(photoId != null)
    var missing by mutableStateOf(false)
    var saving by mutableStateOf(false)
    val routes = repository.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (photoId != null) {
            viewModelScope.launch {
                val photo = repository.getPhoto(photoId)
                if (photo == null) {
                    missing = true
                } else {
                    title = photo.title
                    description = photo.description.orEmpty()
                    location = photo.location.orEmpty()
                    takenAt = photo.takenAt.orEmpty()
                    routeId = photo.routeId
                    imagePath = photo.url
                    originalPath = photo.url
                }
                loading = false
            }
        }
    }

    fun import(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val path = repository.importMedia(appContext, uri, MediaKind.PHOTO)
                val previous = imagePath
                imagePath = path
                if (previous != null && previous != originalPath && previous != path) {
                    repository.discardMedia(previous)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "无法读取照片"
            }
        }
    }

    var message by mutableStateOf<String?>(null)

    fun consumeMessage() {
        message = null
    }

    fun save(onDone: (Long) -> Unit) {
        val path = imagePath
        if (title.isBlank()) {
            message = "请填写标题"
            return
        }
        if (path.isNullOrBlank()) {
            message = "请选择一张照片"
            return
        }
        viewModelScope.launch {
            saving = true
            try {
                val id = repository.savePhoto(
                    PhotoDraft(
                        id = photoId,
                        title = title,
                        description = description,
                        location = location,
                        url = path,
                        routeId = routeId,
                        takenAt = takenAt,
                    ),
                )
                originalPath = path
                onDone(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "保存失败"
            } finally {
                saving = false
            }
        }
    }

    fun abandon() {
        val extra = imagePath
        if (extra != null && extra != originalPath) {
            viewModelScope.launch { repository.discardMedia(extra) }
        }
    }
}

class VideoEditorViewModel(
    private val repository: TravelRepository,
    private val videoId: Long?,
) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var videoPath by mutableStateOf<String?>(null)
    var originalPath by mutableStateOf<String?>(null)
    var loading by mutableStateOf(videoId != null)
    var missing by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    init {
        if (videoId != null) {
            viewModelScope.launch {
                val video = repository.getVideo(videoId)
                if (video == null) {
                    missing = true
                } else {
                    title = video.title
                    description = video.description.orEmpty()
                    videoPath = video.url
                    originalPath = video.url
                }
                loading = false
            }
        }
    }

    fun import(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val path = repository.importMedia(appContext, uri, MediaKind.VIDEO)
                val previous = videoPath
                videoPath = path
                if (previous != null && previous != originalPath && previous != path) {
                    repository.discardMedia(previous)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "无法读取视频"
            }
        }
    }

    fun save(onDone: (Long) -> Unit) {
        val path = videoPath
        if (title.isBlank()) {
            message = "请填写标题"
            return
        }
        if (path.isNullOrBlank()) {
            message = "请选择一个视频"
            return
        }
        viewModelScope.launch {
            saving = true
            try {
                val id = repository.saveVideo(
                    VideoDraft(id = videoId, title = title, description = description, url = path),
                )
                originalPath = path
                onDone(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "保存失败"
            } finally {
                saving = false
            }
        }
    }

    fun abandon() {
        val extra = videoPath
        if (extra != null && extra != originalPath) {
            viewModelScope.launch { repository.discardMedia(extra) }
        }
    }
}

class StoryEditorViewModel(
    private val repository: TravelRepository,
    private val storyId: Long?,
) : ViewModel() {
    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var location by mutableStateOf("")
    var coverPath by mutableStateOf<String?>(null)
    var originalCover by mutableStateOf<String?>(null)
    var loading by mutableStateOf(storyId != null)
    var missing by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    val photos = repository.observePhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routes = repository.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedPhotoIds = mutableStateListOf<Long>()
    val selectedRouteIds = mutableStateListOf<Long>()

    init {
        if (storyId != null) {
            viewModelScope.launch {
                val story = repository.getStory(storyId)
                if (story == null) {
                    missing = true
                } else {
                    title = story.title
                    content = story.content
                    location = story.location.orEmpty()
                    coverPath = story.coverUrl
                    originalCover = story.coverUrl
                    selectedPhotoIds.clear()
                    selectedPhotoIds.addAll(parseIdList(story.photoIds))
                    selectedRouteIds.clear()
                    selectedRouteIds.addAll(parseIdList(story.routeIds))
                }
                loading = false
            }
        }
    }

    fun togglePhoto(photoId: Long) {
        if (photoId in selectedPhotoIds) selectedPhotoIds.remove(photoId) else selectedPhotoIds.add(photoId)
    }

    fun toggleRoute(routeId: Long) {
        if (routeId in selectedRouteIds) selectedRouteIds.remove(routeId) else selectedRouteIds.add(routeId)
    }

    fun importCover(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val path = repository.importMedia(appContext, uri, MediaKind.COVER)
                val previous = coverPath
                coverPath = path
                if (previous != null && previous != originalCover && previous != path) {
                    repository.discardMedia(previous)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "无法读取封面"
            }
        }
    }

    fun clearCover() {
        val previous = coverPath
        coverPath = null
        if (previous != null && previous != originalCover) {
            viewModelScope.launch { repository.discardMedia(previous) }
        }
    }

    fun save(onDone: (Long) -> Unit) {
        if (title.isBlank()) {
            message = "请填写标题"
            return
        }
        if (content.isBlank()) {
            message = "请写下故事内容"
            return
        }
        viewModelScope.launch {
            saving = true
            try {
                val id = repository.saveStory(
                    StoryDraft(
                        id = storyId,
                        title = title,
                        content = content,
                        location = location,
                        coverUrl = coverPath,
                        photoIds = selectedPhotoIds.toList(),
                        routeIds = selectedRouteIds.toList(),
                    ),
                )
                originalCover = coverPath
                onDone(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "保存失败"
            } finally {
                saving = false
            }
        }
    }

    fun abandon() {
        val extra = coverPath
        if (extra != null && extra != originalCover) {
            viewModelScope.launch { repository.discardMedia(extra) }
        }
    }
}

class RouteEditorViewModel(
    private val repository: TravelRepository,
    private val routeId: Long?,
) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var daysText by mutableStateOf("1")
    var coverPath by mutableStateOf<String?>(null)
    var originalCover by mutableStateOf<String?>(null)
    var loading by mutableStateOf(routeId != null)
    var missing by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    val spots = mutableStateListOf<SpotDraft>()

    init {
        if (routeId != null) {
            viewModelScope.launch {
                val details = repository.getRoute(routeId)
                if (details == null) {
                    missing = true
                } else {
                    title = details.route.title
                    description = details.route.description.orEmpty()
                    daysText = details.route.days.toString()
                    coverPath = details.route.coverUrl
                    originalCover = details.route.coverUrl
                    spots.clear()
                    spots.addAll(
                        details.spots.map { spot ->
                            SpotDraft(
                                id = spot.id,
                                name = spot.name,
                                description = spot.description,
                                dayNumber = spot.dayNumber,
                                latitude = spot.latitude,
                                longitude = spot.longitude,
                            )
                        },
                    )
                }
                loading = false
            }
        }
    }

    fun importCover(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val path = repository.importMedia(appContext, uri, MediaKind.COVER)
                val previous = coverPath
                coverPath = path
                if (previous != null && previous != originalCover && previous != path) {
                    repository.discardMedia(previous)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "无法读取封面"
            }
        }
    }

    fun clearCover() {
        val previous = coverPath
        coverPath = null
        if (previous != null && previous != originalCover) {
            viewModelScope.launch { repository.discardMedia(previous) }
        }
    }

    fun addOrReplaceSpot(index: Int?, spot: SpotDraft) {
        if (index == null) spots.add(spot) else spots[index] = spot
    }

    fun removeSpot(index: Int) {
        if (index in spots.indices) spots.removeAt(index)
    }

    fun moveSpot(index: Int, delta: Int) {
        val target = index + delta
        if (index !in spots.indices || target !in spots.indices) return
        val item = spots.removeAt(index)
        spots.add(target, item)
    }

    fun save(onDone: (Long) -> Unit) {
        if (title.isBlank()) {
            message = "请填写标题"
            return
        }
        val days = daysText.toIntOrNull()
        if (days == null || days < 1) {
            message = "请填写至少 1 天"
            return
        }
        viewModelScope.launch {
            saving = true
            try {
                val id = repository.saveRoute(
                    RouteDraft(
                        id = routeId,
                        title = title,
                        description = description,
                        coverUrl = coverPath,
                        days = days,
                        spots = spots.toList(),
                    ),
                )
                originalCover = coverPath
                onDone(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "保存失败"
            } finally {
                saving = false
            }
        }
    }

    fun abandon() {
        val extra = coverPath
        if (extra != null && extra != originalCover) {
            viewModelScope.launch { repository.discardMedia(extra) }
        }
    }
}
