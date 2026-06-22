package com.ytsave.app.di

import android.content.Context
import androidx.room.Room
import com.ytsave.app.data.local.DownloadDao
import com.ytsave.app.data.local.YTSaveDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YTSaveDatabase {
        return Room.databaseBuilder(
            context,
            YTSaveDatabase::class.java,
            "ytsave_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDownloadDao(database: YTSaveDatabase): DownloadDao {
        return database.downloadDao()
    }
}
