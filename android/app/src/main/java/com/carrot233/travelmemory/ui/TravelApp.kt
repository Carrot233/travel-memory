package com.carrot233.travelmemory.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carrot233.travelmemory.ui.home.HomeScreen
import com.carrot233.travelmemory.ui.photos.PhotoDetailScreen
import com.carrot233.travelmemory.ui.photos.PhotoEditScreen
import com.carrot233.travelmemory.ui.photos.PhotoListScreen
import com.carrot233.travelmemory.ui.routes.RouteDetailScreen
import com.carrot233.travelmemory.ui.routes.RouteEditScreen
import com.carrot233.travelmemory.ui.routes.RouteListScreen
import com.carrot233.travelmemory.ui.settings.SettingsScreen
import com.carrot233.travelmemory.ui.stories.StoryDetailScreen
import com.carrot233.travelmemory.ui.stories.StoryEditScreen
import com.carrot233.travelmemory.ui.stories.StoryListScreen
import com.carrot233.travelmemory.ui.videos.VideoDetailScreen
import com.carrot233.travelmemory.ui.videos.VideoEditScreen
import com.carrot233.travelmemory.ui.videos.VideoListScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "首页", Icons.Filled.Home),
    Tab(Routes.PHOTOS, "照片", Icons.Filled.Photo),
    Tab(Routes.ROUTES, "路线", Icons.Filled.Map),
    Tab(Routes.VIDEOS, "视频", Icons.Filled.Videocam),
    Tab(Routes.STORIES, "故事", Icons.AutoMirrored.Filled.MenuBook),
)

fun NavHostController.openSection(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun TravelApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            if (current in Routes.topLevel) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { nav.openSection(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { enterTravel() },
            exitTransition = { exitTravel() },
            popEnterTransition = { popEnterTravel() },
            popExitTransition = { popExitTravel() },
        ) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.PHOTOS) { PhotoListScreen(nav) }
            composable(Routes.PHOTO_NEW) { PhotoEditScreen(nav, null) }
            composable(
                route = Routes.PHOTO_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PhotoEditScreen(nav, entry.arguments?.getLong("id"))
            }
            composable(
                route = Routes.PHOTO,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PhotoDetailScreen(nav, entry.arguments?.getLong("id") ?: return@composable)
            }
            composable(Routes.ROUTES) { RouteListScreen(nav) }
            composable(Routes.ROUTE_NEW) { RouteEditScreen(nav, null) }
            composable(
                route = Routes.ROUTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RouteEditScreen(nav, entry.arguments?.getLong("id"))
            }
            composable(
                route = Routes.ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RouteDetailScreen(nav, entry.arguments?.getLong("id") ?: return@composable)
            }
            composable(Routes.VIDEOS) { VideoListScreen(nav) }
            composable(Routes.VIDEO_NEW) { VideoEditScreen(nav, null) }
            composable(
                route = Routes.VIDEO_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                VideoEditScreen(nav, entry.arguments?.getLong("id"))
            }
            composable(
                route = Routes.VIDEO,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                VideoDetailScreen(nav, entry.arguments?.getLong("id") ?: return@composable)
            }
            composable(Routes.STORIES) { StoryListScreen(nav) }
            composable(Routes.STORY_NEW) { StoryEditScreen(nav, null) }
            composable(
                route = Routes.STORY_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                StoryEditScreen(nav, entry.arguments?.getLong("id"))
            }
            composable(
                route = Routes.STORY,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                StoryDetailScreen(nav, entry.arguments?.getLong("id") ?: return@composable)
            }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabChange(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from in Routes.topLevel && to in Routes.topLevel
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTravel(): EnterTransition {
    val fade = fadeIn(tween(200, easing = FastOutSlowInEasing))
    if (isTabChange()) return fade
    return fade + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 5 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTravel(): ExitTransition {
    val fade = fadeOut(tween(160, easing = FastOutSlowInEasing))
    if (isTabChange()) return fade
    return fade + slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 6 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTravel(): EnterTransition {
    val fade = fadeIn(tween(200, easing = FastOutSlowInEasing))
    if (isTabChange()) return fade
    return fade + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 5 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTravel(): ExitTransition {
    val fade = fadeOut(tween(160, easing = FastOutSlowInEasing))
    if (isTabChange()) return fade
    return fade + slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 5 }
}
