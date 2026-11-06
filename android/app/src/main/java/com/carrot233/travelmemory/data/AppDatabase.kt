package com.carrot233.travelmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotoEntity::class,
        RouteEntity::class,
        RouteSpotEntity::class,
        VideoEntity::class,
        StoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    abstract fun routeDao(): RouteDao

    abstract fun videoDao(): VideoDao

    abstract fun storyDao(): StoryDao

    abstract fun storageDao(): StorageDao

    companion object {
        fun build(context: Context, name: String = "travel_memory.db"): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name).build()
    }
}
