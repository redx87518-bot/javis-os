package com.javis.os.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.javis.os.data.db.dao.AppInfoDao
import com.javis.os.data.db.entities.AppInfoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appInfoDao: AppInfoDao,
    private val knowledgeEngine: AppKnowledgeEngine
) {
    suspend fun scanInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val entities = apps.mapNotNull { info ->
            try {
                val appName = info.loadLabel(pm).toString()
                val packageName = info.activityInfo.packageName
                val profile = knowledgeEngine.getProfile(packageName, appName)
                AppInfoEntity(
                    packageName = packageName,
                    appName = appName,
                    categories = profile.categories.joinToString(","),
                    capabilities = profile.capabilities.joinToString(",")
                )
            } catch (e: Exception) { null }
        }

        appInfoDao.insertAll(entities)
    }

    suspend fun findApp(query: String): AppInfoEntity? = withContext(Dispatchers.IO) {
        appInfoDao.search(query).firstOrNull()
    }

    fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun launchAppWithSearch(packageName: String, query: String): Boolean {
        val searchIntent = buildSearchIntent(packageName, query) ?: return launchApp(packageName)
        return try {
            context.startActivity(searchIntent)
            true
        } catch (e: Exception) {
            launchApp(packageName)
        }
    }

    private fun buildSearchIntent(packageName: String, query: String): Intent? {
        return when (packageName) {
            "com.google.android.youtube" -> Intent(Intent.ACTION_SEARCH).apply {
                setPackage(packageName)
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            "com.android.chrome", "com.google.android.googlequicksearchbox" -> Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            else -> null
        }
    }
}
