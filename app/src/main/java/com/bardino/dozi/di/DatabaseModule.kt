package com.bardino.dozi.di

import android.content.Context
import com.bardino.dozi.core.data.local.DoziDatabase
import com.bardino.dozi.core.data.local.dao.MedicationLogDao
import com.bardino.dozi.core.data.local.dao.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoziDatabase {
        return DoziDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMedicationLogDao(database: DoziDatabase): MedicationLogDao {
        return database.medicationLogDao()
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: DoziDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
}
