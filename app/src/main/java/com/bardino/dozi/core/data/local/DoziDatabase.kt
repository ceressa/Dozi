package com.bardino.dozi.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bardino.dozi.core.data.local.dao.MedicationLogDao
import com.bardino.dozi.core.data.local.dao.ProfileDao
import com.bardino.dozi.core.data.local.dao.SyncQueueDao
import com.bardino.dozi.core.data.local.entity.MedicationLogEntity
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        MedicationLogEntity::class,
        SyncQueueEntity::class,
        ProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DoziDatabase : RoomDatabase() {

    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: DoziDatabase? = null

        // Migration from version 1 to 2: Add profiles table and profileId to medication_logs
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create profiles table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profiles (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        avatarIcon TEXT NOT NULL,
                        color TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Add profileId column to medication_logs
                database.execSQL(
                    "ALTER TABLE medication_logs ADD COLUMN profileId TEXT DEFAULT NULL"
                )

                // Create a default profile for existing users
                val currentTime = System.currentTimeMillis()
                val defaultProfileId = "default-profile"
                database.execSQL(
                    """
                    INSERT INTO profiles (id, name, avatarIcon, color, createdAt, updatedAt, isActive)
                    VALUES ('$defaultProfileId', 'Varsayılan Profil', '👤', '#6200EE', $currentTime, $currentTime, 1)
                    """.trimIndent()
                )

                // Update existing medication_logs to use default profile
                database.execSQL(
                    "UPDATE medication_logs SET profileId = '$defaultProfileId' WHERE profileId IS NULL"
                )
            }
        }

        // Migration from version 2 to 3: Add PIN code support
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add pinCode column to profiles table
                database.execSQL(
                    "ALTER TABLE profiles ADD COLUMN pinCode TEXT DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): DoziDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoziDatabase::class.java,
                    "dozi_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // ⚠️ Only as fallback
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
