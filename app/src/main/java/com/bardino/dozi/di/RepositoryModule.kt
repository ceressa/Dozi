package com.bardino.dozi.di

import android.content.Context
import com.bardino.dozi.core.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBadiRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): BadiRepository {
        return BadiRepository(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideMedicationLogRepository(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): MedicationLogRepository {
        return MedicationLogRepository(context, auth, firestore)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions,
        messaging: FirebaseMessaging
    ): NotificationRepository {
        return NotificationRepository(auth, firestore, functions, messaging)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): UserRepository {
        return UserRepository(auth, firestore)
    }
}
