package com.carrot233.travelmemory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carrot233.travelmemory.data.TravelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryCounts(
    val photos: Int = 0,
    val routes: Int = 0,
    val videos: Int = 0,
    val stories: Int = 0,
)

class LibraryViewModel(repository: TravelRepository) : ViewModel() {
    val photos = repository.observePhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routes = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val videos = repository.observeVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val stories = repository.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val counts = combine(
        repository.observePhotoCount(),
        repository.observeRouteCount(),
        repository.observeVideoCount(),
        repository.observeStoryCount(),
    ) { photoCount, routeCount, videoCount, storyCount ->
        LibraryCounts(photoCount, routeCount, videoCount, storyCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryCounts())
}
