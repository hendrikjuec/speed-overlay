/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.di

import android.content.Context
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.data.SpeedRepository
import com.drgreen.speedoverlay.util.HardwareHelper
import com.drgreen.speedoverlay.util.MotionDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Modul zur Bereitstellung von Daten- und Hilfsklassen.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideLogManager(@ApplicationContext context: Context): LogManager {
        return LogManager(context)
    }

    @Provides
    @Singleton
    fun provideSpeedRepository(): SpeedRepository {
        return SpeedRepository()
    }

    @Provides
    @Singleton
    fun provideHardwareHelper(@ApplicationContext context: Context): HardwareHelper {
        return HardwareHelper(context)
    }

    @Provides
    @Singleton
    fun provideMotionDetector(@ApplicationContext context: Context): MotionDetector {
        return MotionDetector(context)
    }
}
