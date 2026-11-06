package com.carrot233.travelmemory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.carrot233.travelmemory.data.AppDatabase
import com.carrot233.travelmemory.data.MediaFiles
import com.carrot233.travelmemory.data.MediaKind
import com.carrot233.travelmemory.data.PhotoDraft
import com.carrot233.travelmemory.data.RouteDraft
import com.carrot233.travelmemory.data.SpotDraft
import com.carrot233.travelmemory.data.StoryDraft
import com.carrot233.travelmemory.data.TravelRepository
import com.carrot233.travelmemory.data.VideoDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalPersistenceTest {
    @Test
    fun contentSurvivesClosingAndReopeningTheDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "cold-start-${System.nanoTime()}.db"
        val mediaRoot = File(context.filesDir, "test-media-${System.nanoTime()}")
        val media = MediaFiles(mediaRoot)
        var first: AppDatabase? = null
        var second: AppDatabase? = null
        try {
            runBlocking(Dispatchers.IO) {
                first = AppDatabase.build(context, dbName)
                val repository = TravelRepository(first!!, media)
                val routeId = repository.saveRoute(
                    RouteDraft(
                        id = null,
                        title = "杭州两日",
                        description = "湖和寺",
                        coverUrl = null,
                        days = 2,
                        spots = listOf(
                            SpotDraft(null, "西湖", "日落", 1, 30.248, 120.145),
                            SpotDraft(null, "灵隐", null, 2, 30.241, 120.102),
                        ),
                    ),
                )
                val photoPath = media.writeBytes(MediaKind.PHOTO, "jpg", "photo-bytes".toByteArray())
                val photoId = repository.savePhoto(
                    PhotoDraft(null, "西湖日落", "金色", "杭州", photoPath, routeId, "2026-04-01"),
                )
                val storyId = repository.saveStory(
                    StoryDraft(null, "雨中的西湖", "雨还在下", "杭州", null),
                )
                val videoPath = media.writeBytes(MediaKind.VIDEO, "mp4", "video-bytes".toByteArray())
                val videoId = repository.saveVideo(
                    VideoDraft(null, "游船", "湖面", videoPath),
                )
                first!!.close()
                first = null

                second = AppDatabase.build(context, dbName)
                val reopened = TravelRepository(second!!, media)
                val photo = reopened.getPhoto(photoId)
                val story = reopened.getStory(storyId)
                val route = reopened.getRoute(routeId)
                val video = reopened.getVideo(videoId)
                assertNotNull(photo)
                assertEquals("西湖日落", photo?.title)
                assertEquals(routeId, photo?.routeId)
                assertEquals("photo-bytes", media.resolve(photo?.url)?.readBytes()?.decodeToString())
                assertEquals("雨中的西湖", story?.title)
                assertEquals("雨还在下", story?.content)
                assertEquals("杭州两日", route?.route?.title)
                assertEquals(listOf("西湖", "灵隐"), route?.spots?.map { it.name })
                assertEquals("游船", video?.title)
                assertEquals("video-bytes", media.resolve(video?.url)?.readBytes()?.decodeToString())

                reopened.deleteRoute(routeId)
                assertNull(reopened.getRoute(routeId))
                assertEquals(0, reopened.spotCount(routeId))
                val photoAfter = reopened.getPhoto(photoId)
                assertNotNull(photoAfter)
                assertNull(photoAfter?.routeId)
                assertEquals("西湖日落", photoAfter?.title)
            }
        } finally {
            first?.close()
            second?.close()
            context.deleteDatabase(dbName)
            mediaRoot.deleteRecursively()
        }
    }

    @Test
    fun appIsOfflineAndUsesTheTravelMemoryLabel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions?.toSet().orEmpty()
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
        assertEquals("旅途记忆", context.applicationInfo.loadLabel(context.packageManager).toString())
    }
}
