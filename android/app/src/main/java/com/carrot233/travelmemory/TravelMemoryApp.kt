package com.carrot233.travelmemory

import android.app.Application
import android.content.Context
import com.carrot233.travelmemory.data.AppDatabase
import com.carrot233.travelmemory.data.MediaFiles
import com.carrot233.travelmemory.data.TravelRepository
import com.carrot233.travelmemory.data.UserSettings

class TravelMemoryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val repository: TravelRepository by lazy { TravelRepository(database, MediaFiles(appContext.filesDir)) }
    val settings = UserSettings(appContext)
}

fun Context.travelApp(): TravelMemoryApp = applicationContext as TravelMemoryApp
