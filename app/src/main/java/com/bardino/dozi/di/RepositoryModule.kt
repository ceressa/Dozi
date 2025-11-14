package com.bardino.dozi.di

import com.bardino.dozi.core.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideBuddyRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): BuddyRepository {
        return BuddyRepository(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideMedicationLogRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): MedicationLogRepository {
        return MedicationLogRepository(auth, firestore)
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
