package com.carrot233.travelmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PhotoEntity::class,
        RouteEntity::class,
        RouteSpotEntity::class,
        VideoEntity::class,
        StoryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    abstract fun routeDao(): RouteDao

    abstract fun videoDao(): VideoDao

    abstract fun storyDao(): StoryDao

    abstract fun storageDao(): StorageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stories ADD COLUMN photo_ids TEXT")
                db.execSQL("ALTER TABLE stories ADD COLUMN route_ids TEXT")
            }
        }

        fun build(context: Context, name: String = "travel_memory.db"): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
