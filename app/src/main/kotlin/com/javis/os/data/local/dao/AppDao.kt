package com.javis.os.data.local.dao

import androidx.room.*
import com.javis.os.data.local.entities.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM installed_apps WHERE LOWER(appName) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchApps(query: String): List<AppEntity>

    @Query("SELECT * FROM installed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Query("DELETE FROM installed_apps WHERE packageName NOT IN (:packageNames)")
    suspend fun removeUninstalled(packageNames: List<String>)
}
