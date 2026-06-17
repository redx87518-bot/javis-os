package com.javis.os.di

import android.content.Context
import androidx.room.Room
import com.javis.os.BuildConfig
import com.javis.os.ai.AiProvider
import com.javis.os.ai.GeminiProvider
import com.javis.os.data.db.AppDatabase
import com.javis.os.data.db.dao.AppInfoDao
import com.javis.os.data.db.dao.ConversationDao
import com.javis.os.data.db.dao.MemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMemoryDao(db: AppDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideAppInfoDao(db: AppDatabase): AppInfoDao = db.appInfoDao()

    @Provides
    @Singleton
    fun provideDefaultAiProvider(): AiProvider {
        return GeminiProvider(BuildConfig.GEMINI_API_KEY)
    }
}
