package com.carrot233.travelmemory.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalSharedTransitionApi::class)
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
                forwardEnter()
            }
            val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                forwardExit()
            }
            val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                backEnter()
            }
            val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                backExit()
            }
            val predictiveEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
                backEnter()
            }
            val predictiveExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { edge ->
                predictiveBackExit(edge)
            }
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
                predictivePopEnterTransition = predictiveEnter,
                predictivePopExitTransition = predictiveExit,
                sizeTransform = { null },
            ) {
                composable(Routes.HOME) { Motion { TabPage { HomeScreen(nav) } } }
                composable(Routes.PHOTOS) { Motion { TabPage { PhotoListScreen(nav) } } }
                composable(Routes.PHOTO_NEW) { Motion { Page { PhotoEditScreen(nav, null) } } }
                composable(
                    route = Routes.PHOTO_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Motion { Page { PhotoEditScreen(nav, entry.arguments?.getLong("id")) } }
                }
                composable(
                    route = Routes.PHOTO,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Motion { ViewerPage { PhotoDetailScreen(nav, id) } }
                }
                composable(Routes.ROUTES) { Motion { TabPage { RouteListScreen(nav) } } }
                composable(Routes.ROUTE_NEW) { Motion { Page { RouteEditScreen(nav, null) } } }
                composable(
                    route = Routes.ROUTE_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Motion { Page { RouteEditScreen(nav, entry.arguments?.getLong("id")) } }
                }
                composable(
                    route = Routes.ROUTE,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Motion { Page { RouteDetailScreen(nav, id) } }
                }
                composable(Routes.VIDEOS) { Motion { TabPage { VideoListScreen(nav) } } }
                composable(Routes.VIDEO_NEW) { Motion { Page { VideoEditScreen(nav, null) } } }
                composable(
                    route = Routes.VIDEO_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Motion { Page { VideoEditScreen(nav, entry.arguments?.getLong("id")) } }
                }
                composable(
                    route = Routes.VIDEO,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Motion { Page { VideoDetailScreen(nav, id) } }
                }
                composable(Routes.STORIES) { Motion { TabPage { StoryListScreen(nav) } } }
                composable(Routes.STORY_NEW) { Motion { Page { StoryEditScreen(nav, null) } } }
                composable(
                    route = Routes.STORY_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    Motion { Page { StoryEditScreen(nav, entry.arguments?.getLong("id")) } }
                }
                composable(
                    route = Routes.STORY,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    Motion { Page { StoryDetailScreen(nav, id) } }
                }
                composable(Routes.SETTINGS) { Motion { Page { SettingsScreen(nav) } } }
            }
            }
            }
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
private fun AnimatedContentScope.Motion(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavMotion provides this, content = content)
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
private fun ViewerPage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) { content() }
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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.openingPhoto(): Boolean =
    isPhotoViewer(targetState.destination.route) && !isPhotoViewer(initialState.destination.route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.closingPhoto(): Boolean =
    isPhotoViewer(initialState.destination.route) && !isPhotoViewer(targetState.destination.route)

internal fun isPhotoViewerRoute(route: String?): Boolean {
    if (route == null || route == Routes.PHOTOS || route == Routes.PHOTO_NEW || route == Routes.PHOTO_EDIT) {
        return false
    }
    if (route == Routes.PHOTO) return true
    if (!route.startsWith("photo/") || route.endsWith("/edit")) return false
    return route.removePrefix("photo/").all { it.isDigit() }
}

private fun isPhotoViewer(route: String?): Boolean = isPhotoViewerRoute(route)

private fun tabEnter(): EnterTransition = fadeIn(tween(200, easing = FastOutSlowInEasing))

private fun tabExit(): ExitTransition = fadeOut(tween(160, easing = FastOutSlowInEasing))

private fun photoOpenEnter(): EnterTransition = fadeIn(weChatFade)

private fun photoOpenExit(): ExitTransition = fadeOut(weChatFade, targetAlpha = 0.55f)

private fun photoCloseEnter(): EnterTransition = fadeIn(weChatFade, initialAlpha = 0.55f)

private fun photoCloseExit(): ExitTransition = fadeOut(weChatFade)

private fun pageEnter(): EnterTransition = slideInHorizontally(weChatSlide) { width -> width }

private fun pageExit(): ExitTransition =
    slideOutHorizontally(weChatSlide) { width -> -width * 3 / 10 } + fadeOut(weChatFade, targetAlpha = 0.78f)

private fun pagePopEnter(): EnterTransition =
    slideInHorizontally(weChatSlide) { width -> -width * 3 / 10 } + fadeIn(weChatFade, initialAlpha = 0.78f)

private fun pagePopExit(): ExitTransition = slideOutHorizontally(weChatSlide) { width -> width }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition = when {
    isTabChange() -> tabEnter()
    openingPhoto() -> photoOpenEnter()
    else -> pageEnter()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition = when {
    isTabChange() -> tabExit()
    openingPhoto() -> photoOpenExit()
    else -> pageExit()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition = when {
    isTabChange() -> tabEnter()
    closingPhoto() -> photoCloseEnter()
    else -> pagePopEnter()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition = when {
    isTabChange() -> tabExit()
    closingPhoto() -> photoCloseExit()
    else -> pagePopExit()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.predictiveBackExit(edge: Int): ExitTransition = when {
    isTabChange() -> tabExit()
    closingPhoto() -> photoCloseExit()
    edge == 1 -> slideOutHorizontally(weChatSlide) { width -> -width }
    else -> pagePopExit()
}

private val weChatSlide = tween<androidx.compose.ui.unit.IntOffset>(WeChatDurationMs, easing = WeChatEasing)
private val weChatFade = tween<Float>(WeChatDurationMs, easing = WeChatEasing)
