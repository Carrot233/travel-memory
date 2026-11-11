package com.carrot233.travelmemory.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavMotion = staticCompositionLocalOf<AnimatedContentScope?> { null }

/** WeChat / iOS-style ease-out. Fast start, gentle settle. */
val WeChatEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
const val WeChatDurationMs = 300

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.photoSharedBounds(photoId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val motion = LocalNavMotion.current ?: return this
    return with(shared) {
        sharedElement(
            sharedContentState = rememberSharedContentState(key = "photo-$photoId"),
            animatedVisibilityScope = motion,
            boundsTransform = BoundsTransform { _, _ ->
                tween(durationMillis = WeChatDurationMs, easing = WeChatEasing)
            },
        )
    }
}
