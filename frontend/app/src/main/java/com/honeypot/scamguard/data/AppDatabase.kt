package com.honeypot.scamguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.honeypot.scamguard.data.dao.ScamEventDao
import com.honeypot.scamguard.data.dao.ScammerDao
import com.honeypot.scamguard.data.entity.ScamEvent
import com.honeypot.scamguard.data.entity.ScammerProfile

@Database(entities = [ScammerProfile::class, ScamEvent::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scammerDao(): ScammerDao
    abstract fun scamEventDao(): ScamEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scamguard_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
