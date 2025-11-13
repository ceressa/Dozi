package com.dozi.dozi.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Eğer Room DAO ve Database sınıflarını eklediğinde çalışır.
// Şimdilik iskelet: derleme hatası olmaması için yorumlu bırakıldı.
/*
@Database(entities = [Medicine::class], version = 1)
abstract class DoziDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
}
*/

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Örnek sağlayıcı – Room eklenince aç
    /*
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): DoziDatabase =
        Room.databaseBuilder(ctx, DoziDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    */
}
