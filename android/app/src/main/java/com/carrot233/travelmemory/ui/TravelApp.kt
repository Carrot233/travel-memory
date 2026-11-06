package com.carrot233.travelmemory.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    // The bar floats over a stable content area. Hiding it must not resize the
    // page underneath, or list-to-detail (especially the photo viewer) snaps.
    Box(Modifier.fillMaxSize()) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
            val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                if (isTabChange()) tabEnter() else pageEnter()
            }
            val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                if (isTabChange()) tabExit() else pageExit()
            }
            val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                if (isTabChange()) tabEnter() else pagePopEnter()
            }
            val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                if (isTabChange()) tabExit() else pagePopExit()
            }
            val predictiveEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
                if (isTabChange()) tabEnter() else pagePopEnter()
            }
            val predictiveExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { edge ->
                if (isTabChange()) tabExit() else pagePredictivePopExit(edge)
            }
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding).fillMaxSize(),
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
                predictivePopEnterTransition = predictiveEnter,
                predictivePopExitTransition = predictiveExit,
                sizeTransform = { null },
            ) {
                composable(Routes.HOME) { TabPage { HomeScreen(nav) } }
                composable(Routes.PHOTOS) { TabPage { PhotoListScreen(nav) } }
                composable(Routes.PHOTO_NEW) { Page { PhotoEditScreen(nav, null) } }
                composable(
                    route = Routes.PHOTO_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Page { PhotoEditScreen(nav, entry.arguments?.getLong("id")) }
                }
                composable(
                    route = Routes.PHOTO,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Page { PhotoDetailScreen(nav, id) }
                }
                composable(Routes.ROUTES) { TabPage { RouteListScreen(nav) } }
                composable(Routes.ROUTE_NEW) { Page { RouteEditScreen(nav, null) } }
                composable(
                    route = Routes.ROUTE_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Page { RouteEditScreen(nav, entry.arguments?.getLong("id")) }
                }
                composable(
                    route = Routes.ROUTE,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Page { RouteDetailScreen(nav, id) }
                }
                composable(Routes.VIDEOS) { TabPage { VideoListScreen(nav) } }
                composable(Routes.VIDEO_NEW) { Page { VideoEditScreen(nav, null) } }
                composable(
                    route = Routes.VIDEO_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Page { VideoEditScreen(nav, entry.arguments?.getLong("id")) }
                }
                composable(
                    route = Routes.VIDEO,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Page { VideoDetailScreen(nav, id) }
                }
                composable(Routes.STORIES) { TabPage { StoryListScreen(nav) } }
                composable(Routes.STORY_NEW) { Page { StoryEditScreen(nav, null) } }
                composable(
                    route = Routes.STORY_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Page { StoryEditScreen(nav, entry.arguments?.getLong("id")) }
                }
                composable(
                    route = Routes.STORY,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Page { StoryDetailScreen(nav, id) }
                }
                composable(Routes.SETTINGS) { Page { SettingsScreen(nav) } }
            }
        }
        AnimatedVisibility(
            visible = current in Routes.topLevel,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(140, easing = FastOutSlowInEasing)),
        ) {
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
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

@Composable
private fun TabPage(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 80.dp),
    ) {
        content()
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabChange(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from in Routes.topLevel && to in Routes.topLevel
}

private fun tabEnter(): EnterTransition = fadeIn(tween(200, easing = FastOutSlowInEasing))

private fun tabExit(): ExitTransition = fadeOut(tween(160, easing = FastOutSlowInEasing))

private fun pageEnter(): EnterTransition =
    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> width }

private fun pageExit(): ExitTransition =
    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> -width }

private fun pagePopEnter(): EnterTransition =
    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> -width }

private fun pagePopExit(): ExitTransition =
    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> width }

private fun pagePredictivePopExit(edge: Int): ExitTransition {
    // 0 follows a swipe from the start edge; 1 follows a swipe from the end edge.
    return if (edge == 1) pageExit() else pagePopExit()
}
