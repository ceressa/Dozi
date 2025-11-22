package com.bardino.dozi.di

import android.content.Context
import com.bardino.dozi.notifications.ReminderEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideReminderEngine(
        @ApplicationContext context: Context
    ): ReminderEngine {
        return ReminderEngine(context)
    }
}
