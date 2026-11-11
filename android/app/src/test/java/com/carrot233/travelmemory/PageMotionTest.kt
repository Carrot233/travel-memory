package com.carrot233.travelmemory

import com.carrot233.travelmemory.ui.Routes
import com.carrot233.travelmemory.ui.isPhotoViewerRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageMotionTest {
    @Test
    fun photoViewerIsOnlyTheFullscreenRoute() {
        assertTrue(isPhotoViewerRoute(Routes.PHOTO))
        assertTrue(isPhotoViewerRoute("photo/12"))
        assertFalse(isPhotoViewerRoute(Routes.PHOTOS))
        assertFalse(isPhotoViewerRoute(Routes.PHOTO_NEW))
        assertFalse(isPhotoViewerRoute(Routes.PHOTO_EDIT))
        assertFalse(isPhotoViewerRoute("photo/12/edit"))
        assertFalse(isPhotoViewerRoute(Routes.STORY))
        assertFalse(isPhotoViewerRoute(Routes.SETTINGS))
        assertFalse(isPhotoViewerRoute(null))
    }
}
