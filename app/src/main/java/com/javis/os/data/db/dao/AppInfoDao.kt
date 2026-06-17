package com.javis.os.data.db.dao

import androidx.room.*
import com.javis.os.data.db.entities.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInfoEntity)

    @Query("SELECT * FROM app_info WHERE LOWER(appName) LIKE '%' || LOWER(:query) || '%'")
    suspend fun search(query: String): List<AppInfoEntity>

    @Query("SELECT * FROM app_info ORDER BY lastUsed DESC")
    fun getAllFlow(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppInfoEntity?

    @Query("SELECT COUNT(*) FROM app_info")
    suspend fun count(): Int

    @Query("DELETE FROM app_info")
    suspend fun clearAll()
}
