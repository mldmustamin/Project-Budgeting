package com.example.fundsmanager.di

import com.example.fundsmanager.data.repository.FundsRepositoryImpl
import com.example.fundsmanager.data.service.FileStorageServiceImpl
import com.example.fundsmanager.data.service.ReportFileRepositoryImpl
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.service.FileStorageService
import com.example.fundsmanager.domain.service.ReportFileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFundsRepository(
        fundsRepositoryImpl: FundsRepositoryImpl
    ): FundsRepository

    @Binds
    @Singleton
    abstract fun bindFileStorageService(
        fileStorageServiceImpl: FileStorageServiceImpl
    ): FileStorageService

    @Binds
    @Singleton
    abstract fun bindReportFileRepository(
        reportFileRepositoryImpl: ReportFileRepositoryImpl
    ): ReportFileRepository
}
