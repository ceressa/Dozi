package com.bardino.dozi.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bardino.dozi.core.data.local.dao.MedicationLogDao
import com.bardino.dozi.core.data.local.dao.SyncQueueDao
import com.bardino.dozi.core.data.local.entity.MedicationLogEntity
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        MedicationLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DoziDatabase : RoomDatabase() {

    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: DoziDatabase? = null

        fun getDatabase(context: Context): DoziDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoziDatabase::class.java,
                    "dozi_database"
                )
                    .fallbackToDestructiveMigration() // ⚠️ Production'da migration strategy kullan
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
