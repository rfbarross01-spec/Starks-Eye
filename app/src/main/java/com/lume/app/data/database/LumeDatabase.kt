package com.lume.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = false)
abstract class LumeDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile private var INSTANCE: LumeDatabase? = null

        fun get(context: Context): LumeDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                LumeDatabase::class.java,
                "lume_database"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
