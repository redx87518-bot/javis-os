package com.javis.os.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.javis.os.BuildConfig
import com.javis.os.data.local.JavisDatabase
import com.javis.os.data.local.dao.*
import com.javis.os.data.remote.api.DeepSeekApi
import com.javis.os.data.remote.api.ElevenLabsApi
import com.javis.os.data.remote.api.GroqApi
import com.javis.os.data.repository.AiRepositoryImpl
import com.javis.os.data.repository.ConversationRepositoryImpl
import com.javis.os.data.repository.MemoryRepositoryImpl
import com.javis.os.domain.repository.AiRepository
import com.javis.os.domain.repository.ConversationRepository
import com.javis.os.domain.repository.MemoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): JavisDatabase =
        Room.databaseBuilder(ctx, JavisDatabase::class.java, "javis_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideConversationDao(db: JavisDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMemoryDao(db: JavisDatabase): MemoryDao = db.memoryDao()
    @Provides fun provideAppDao(db: JavisDatabase): AppDao = db.appDao()
    @Provides fun provideAlarmDao(db: JavisDatabase): AlarmDao = db.alarmDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.GROQ_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    @Provides
    @Singleton
    @Named("elevenlabs")
    fun provideElevenLabsRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.ELEVENLABS_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    @Provides @Singleton
    fun provideGroqApi(@Named("groq") retrofit: Retrofit): GroqApi =
        retrofit.create(GroqApi::class.java)

    @Provides @Singleton
    fun provideDeepSeekApi(@Named("deepseek") retrofit: Retrofit): DeepSeekApi =
        retrofit.create(DeepSeekApi::class.java)

    @Provides @Singleton
    fun provideElevenLabsApi(@Named("elevenlabs") retrofit: Retrofit): ElevenLabsApi =
        retrofit.create(ElevenLabsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
    @Binds @Singleton abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository
    @Binds @Singleton abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
}
