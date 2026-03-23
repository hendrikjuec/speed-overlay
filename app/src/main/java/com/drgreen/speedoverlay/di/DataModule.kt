package com.drgreen.speedoverlay.di

import android.content.Context
import androidx.room.Room
import com.drgreen.speedoverlay.data.OverpassApi
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.data.SpeedDatabase
import com.drgreen.speedoverlay.data.SpeedRepository
import com.drgreen.speedoverlay.logic.OsmParser
import com.drgreen.speedoverlay.util.HardwareHelper
import com.drgreen.speedoverlay.util.MotionDetector
import com.drgreen.speedoverlay.util.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

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
    fun provideOsmParser(): OsmParser {
        return OsmParser()
    }

    @Provides
    @Singleton
    fun provideSpeedDatabase(@ApplicationContext context: Context): SpeedDatabase {
        return Room.databaseBuilder(
            context,
            SpeedDatabase::class.java,
            "speed_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideOverpassApi(): OverpassApi {
        return Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSpeedRepository(
        overpassApi: OverpassApi,
        osmParser: OsmParser,
        speedDatabase: SpeedDatabase
    ): SpeedRepository {
        return SpeedRepository(overpassApi, osmParser, speedDatabase)
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

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }
}
