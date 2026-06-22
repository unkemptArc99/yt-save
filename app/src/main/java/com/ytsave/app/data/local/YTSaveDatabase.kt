package com.ytsave.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class YTSaveDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
