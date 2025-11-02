package com.mcallzbl.apklist.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.mcallzbl.apklist.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {

    private val packageManager = context.packageManager

    /**
     * 获取已安装的应用列表
     * @param includeSystemApps 是否包含系统应用
     * @return 应用信息列表
     */
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedPackages
                .filter { appInfo ->
                    // 过滤系统应用（如果需要）
                    includeSystemApps || !isSystemApp(appInfo)
                }
                .mapNotNull { appInfo ->
                    try {
                        createAppInfo(appInfo)
                    } catch (e: Exception) {
                        null // 跳过无法获取信息的应用
                    }
                }
                .sortedBy { it.appName.lowercase() }
        }

    /**
     * 创建应用信息对象
     */
    private fun createAppInfo(appInfo: ApplicationInfo): AppInfo? {
        return try {
            val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = getApplicationIcon(appInfo)

            AppInfo(
                appName = appName,
                packageName = appInfo.packageName,
                versionName = packageInfo.versionName ?: "未知版本",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                icon = icon,
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                isSystemApp = isSystemApp(appInfo)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用图标
     */
    private fun getApplicationIcon(appInfo: ApplicationInfo): Drawable? {
        return try {
            packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断是否为系统应用
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    /**
     * 搜索应用
     */
    suspend fun searchApps(query: String, includeSystemApps: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                getInstalledApps(includeSystemApps)
            } else {
                getInstalledApps(includeSystemApps).filter { app ->
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                }
            }
        }
}