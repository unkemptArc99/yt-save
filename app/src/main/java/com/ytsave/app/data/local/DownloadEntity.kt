package com.ytsave.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class DownloadStatus {
    QUEUED, DOWNLOADING, MERGING, COMPLETED, FAILED, CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String? = null,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val duration: Long = 0,
    val format: String = "video+audio",
    val quality: String = "best",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val downloadSpeed: String? = null,
    val eta: String? = null,
    val errorMessage: String? = null,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'MERGING', 'FAILED')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY downloadedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("""
        SELECT * FROM downloads
        WHERE status = 'COMPLETED'
        AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(channel) LIKE '%' || LOWER(:query) || '%')
        ORDER BY downloadedAt DESC
    """)
    fun searchDownloads(query: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT SUM(fileSize) FROM downloads WHERE status = 'COMPLETED'")
    fun getTotalStorageUsed(): Flow<Long?>
}
