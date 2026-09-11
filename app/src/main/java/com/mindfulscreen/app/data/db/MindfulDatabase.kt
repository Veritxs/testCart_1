package com.mindfulscreen.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppLimitEntity::class, DailyScoreEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MindfulDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun dailyScoreDao(): DailyScoreDao

    companion object {
        @Volatile private var INSTANCE: MindfulDatabase? = null

        fun get(context: Context): MindfulDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MindfulDatabase::class.java,
                    "mindful.db"
                ).build().also { INSTANCE = it }
            }
    }
}
