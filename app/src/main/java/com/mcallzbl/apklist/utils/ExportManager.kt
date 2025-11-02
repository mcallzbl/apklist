package com.mcallzbl.apklist.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.mcallzbl.apklist.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val error: String? = null,
    val exportedCount: Int = 0,
    val mimeType: String? = null
)

class ExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * 导出应用列表到JSON格式
     */
    suspend fun exportToJson(apps: List<AppInfo>, fileName: String? = null): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                val jsonContent = generateJsonContent(apps)
                val file = saveToFile(jsonContent, fileName ?: "app_list_${dateFormat.format(Date())}.json", "json")
                ExportResult(success = true, filePath = file.absolutePath, exportedCount = apps.size, mimeType = "application/json")
            } catch (e: Exception) {
                ExportResult(success = false, error = "导出JSON失败: ${e.message}")
            }
        }

    /**
     * 导出应用列表到CSV格式
     */
    suspend fun exportToCsv(apps: List<AppInfo>, fileName: String? = null): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                val csvContent = generateCsvContent(apps)
                val file = saveToFile(csvContent, fileName ?: "app_list_${dateFormat.format(Date())}.csv", "csv")
                ExportResult(success = true, filePath = file.absolutePath, exportedCount = apps.size, mimeType = "text/csv")
            } catch (e: Exception) {
                ExportResult(success = false, error = "导出CSV失败: ${e.message}")
            }
        }

    /**
     * 导出应用列表到TXT格式
     */
    suspend fun exportToTxt(apps: List<AppInfo>, fileName: String? = null): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                val txtContent = generateTxtContent(apps)
                val file = saveToFile(txtContent, fileName ?: "app_list_${dateFormat.format(Date())}.txt", "txt")
                ExportResult(success = true, filePath = file.absolutePath, exportedCount = apps.size, mimeType = "text/plain")
            } catch (e: Exception) {
                ExportResult(success = false, error = "导出TXT失败: ${e.message}")
            }
        }

    /**
     * 生成JSON内容
     */
    private fun generateJsonContent(apps: List<AppInfo>): String {
        val jsonApps = apps.map { app ->
            """{
                "appName": "${escapeJson(app.appName)}",
                "packageName": "${app.packageName}",
                "versionName": "${escapeJson(app.versionName)}",
                "versionCode": ${app.versionCode},
                "isSystemApp": ${app.isSystemApp},
                "installTime": ${app.installTime},
                "updateTime": ${app.updateTime}
            }""".trimIndent()
        }

        return """{
            "exportTime": "${Date()}",
            "totalApps": ${apps.size},
            "apps": [
                ${jsonApps.joinToString(",\n")}
            ]
        }"""
    }

    /**
     * 生成CSV内容
     */
    private fun generateCsvContent(apps: List<AppInfo>): String {
        val header = "应用名称,包名,版本号,版本代码,系统应用,安装时间,更新时间\n"
        val rows = apps.map { app ->
            val installDate = if (app.installTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.installTime)) else "未知"
            val updateDate = if (app.updateTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.updateTime)) else "未知"

            "${escapeCsv(app.appName)},${escapeCsv(app.packageName)},${escapeCsv(app.versionName)},${app.versionCode},${app.isSystemApp},$installDate,$updateDate"
        }

        return header + rows.joinToString("\n")
    }

    /**
     * 生成TXT内容
     */
    private fun generateTxtContent(apps: List<AppInfo>): String {
        val header = "应用列表导出\n导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n应用总数: ${apps.size}\n${"=".repeat(80)}\n\n"

        val appDetails = apps.mapIndexed { index, app ->
            val installDate = if (app.installTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.installTime)) else "未知"
            val updateDate = if (app.updateTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.updateTime)) else "未知"

            """${index + 1}. ${app.appName}
   包名: ${app.packageName}
   版本: ${app.versionName} (${app.versionCode})
   类型: ${if (app.isSystemApp) "系统应用" else "用户应用"}
   安装时间: $installDate
   更新时间: $updateDate
   ${"-".repeat(40)}"""
        }

        return header + appDetails.joinToString("\n\n")
    }

    /**
     * 保存文件到外部存储
     */
    private fun saveToFile(content: String, fileName: String, extension: String): File {
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ApkList")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { output ->
            output.write(content.toByteArray(charset("UTF-8")))
        }
        return file
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t")
    }

    /**
     * 转义CSV字符串中的特殊字符
     */
    private fun escapeCsv(str: String): String {
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"${str.replace("\"", "\"\"")}\""
        } else {
            str
        }
    }

    /**
     * 分享文件
     */
    fun shareFile(context: Context, filePath: String, mimeType: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "分享应用列表：共包含 ${getAppCountFromFileName(file.name)} 个应用")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "分享应用列表")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从文件名中提取应用数量
     */
    private fun getAppCountFromFileName(fileName: String): String {
        return try {
            // 如果文件名包含数量信息，提取它
            val regex = """_(\d+)_""".toRegex()
            regex.find(fileName)?.groupValues?.get(1) ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }
}