package com.carrot233.travelmemory.data

data class PhotoDraft(
    val id: Long?,
    val title: String,
    val description: String?,
    val location: String?,
    val url: String,
    val routeId: Long?,
    val takenAt: String?,
)

data class VideoDraft(
    val id: Long?,
    val title: String,
    val description: String?,
    val url: String,
)

data class StoryDraft(
    val id: Long?,
    val title: String,
    val content: String,
    val location: String?,
    val coverUrl: String?,
)

data class SpotDraft(
    val id: Long?,
    val name: String,
    val description: String?,
    val dayNumber: Int,
    val latitude: Double?,
    val longitude: Double?,
)

data class RouteDraft(
    val id: Long?,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val days: Int,
    val spots: List<SpotDraft>,
)

data class RouteDetails(
    val route: RouteEntity,
    val spots: List<RouteSpotEntity>,
    val photos: List<PhotoEntity>,
)
