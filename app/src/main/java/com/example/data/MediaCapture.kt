package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import kotlinx.coroutines.flow.Flow

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class BackupStatus {
    LOCAL_ONLY,
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED
}

@Entity(tableName = "media_captures")
data class MediaCapture(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val mediaType: MediaType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val backupStatus: BackupStatus = BackupStatus.LOCAL_ONLY,
    val cloudKey: String? = null,
    val cloudUrl: String? = null,
    val locationNote: String? = null
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_captures ORDER BY timestamp DESC")
    fun getAllCaptures(): Flow<List<MediaCapture>>

    @Query("SELECT * FROM media_captures WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteCaptures(): Flow<List<MediaCapture>>

    @Query("SELECT * FROM media_captures WHERE id = :id LIMIT 1")
    suspend fun getCaptureById(id: Long): MediaCapture?

    @Query("SELECT * FROM media_captures WHERE backupStatus != 'SYNCED' ORDER BY timestamp DESC")
    suspend fun getPendingBackupCaptures(): List<MediaCapture>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: MediaCapture): Long

    @Update
    suspend fun updateCapture(capture: MediaCapture)

    @Delete
    suspend fun deleteCapture(capture: MediaCapture)

    @Query("DELETE FROM media_captures WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [MediaCapture::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "instant_cam_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
