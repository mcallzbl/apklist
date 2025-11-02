package com.mcallzbl.apklist.di

import android.app.Application
import com.mcallzbl.apklist.utils.AppManager
import com.mcallzbl.apklist.utils.ExportManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppManager(application: Application): AppManager {
        return AppManager(application)
    }

    @Provides
    @Singleton
    fun provideExportManager(application: Application): ExportManager {
        return ExportManager(application)
    }
}