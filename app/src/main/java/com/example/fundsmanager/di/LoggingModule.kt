package com.example.fundsmanager.di

import android.content.Context
import com.example.fundsmanager.util.logging.AppLogger
import com.example.fundsmanager.util.logging.FileAppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @Singleton
    fun provideAppLogger(@ApplicationContext context: Context): AppLogger {
        return FileAppLogger(context)
    }
}
