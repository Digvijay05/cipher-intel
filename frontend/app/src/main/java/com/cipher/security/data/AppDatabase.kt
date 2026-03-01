package com.cipher.security.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cipher.security.data.dao.EngagementDao
import com.cipher.security.data.dao.ScamEventDao
import com.cipher.security.data.dao.ScammerDao
import com.cipher.security.data.dao.ThreatDao
import com.cipher.security.data.entity.EngagementMessage
import com.cipher.security.data.entity.EngagementSession
import com.cipher.security.data.entity.ScamEvent
import com.cipher.security.data.entity.ScammerProfile
import com.cipher.security.data.entity.ThreatEntity

@Database(
    entities = [
        ScammerProfile::class,
        ScamEvent::class,
        ThreatEntity::class,
        EngagementSession::class,
        EngagementMessage::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scammerDao(): ScammerDao
    abstract fun scamEventDao(): ScamEventDao
    abstract fun threatDao(): ThreatDao
    abstract fun engagementDao(): EngagementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cipher_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
