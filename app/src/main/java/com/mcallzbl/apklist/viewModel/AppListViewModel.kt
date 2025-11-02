package com.mcallzbl.apklist.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcallzbl.apklist.model.AppInfo
import com.mcallzbl.apklist.utils.AppManager
import com.mcallzbl.apklist.utils.ExportManager
import com.mcallzbl.apklist.utils.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppListUiState(
    val isLoading: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val includeSystemApps: Boolean = false,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appManager: AppManager,
    private val exportManager: ExportManager,
    private val application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    /**
     * 加载应用列表
     */
    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val apps = appManager.getInstalledApps(_uiState.value.includeSystemApps)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    apps = apps,
                    filteredApps = apps,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载应用列表失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 搜索应用
     */
    fun searchApps(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        viewModelScope.launch {
            try {
                val filteredApps = appManager.searchApps(query, _uiState.value.includeSystemApps)
                _uiState.value = _uiState.value.copy(
                    filteredApps = filteredApps,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "搜索失败: ${e.message}")
            }
        }
    }

    /**
     * 切换是否显示系统应用
     */
    fun toggleSystemApps() {
        val newValue = !_uiState.value.includeSystemApps
        _uiState.value = _uiState.value.copy(includeSystemApps = newValue)
        loadApps()
    }

    /**
     * 导出为JSON
     */
    fun exportToJson() {
        exportApps { apps, fileName -> exportManager.exportToJson(apps, fileName) }
    }

    /**
     * 导出为CSV
     */
    fun exportToCsv() {
        exportApps { apps, fileName -> exportManager.exportToCsv(apps, fileName) }
    }

    /**
     * 导出为TXT
     */
    fun exportToTxt() {
        exportApps { apps, fileName -> exportManager.exportToTxt(apps, fileName) }
    }

    /**
     * 通用的导出方法
     */
    private fun exportApps(exportFunction: suspend (List<AppInfo>, String?) -> ExportResult) {
        viewModelScope.launch {
            val appsToExport = _uiState.value.filteredApps

            if (appsToExport.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "没有应用可以导出"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null, error = null)

            try {
                val fileName = "app_list_${_uiState.value.searchQuery.ifBlank { "all" }}_${System.currentTimeMillis()}"
                val result = exportFunction(appsToExport, fileName)

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = if (result.success) {
                        "成功导出 ${result.exportedCount} 个应用到:\n${result.filePath}"
                    } else {
                        "导出失败: ${result.error}"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "导出过程中发生错误: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除导出消息
     */
    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    /**
     * 导出为JSON并分享
     */
    fun exportAndShareJson() {
        exportAndShare { apps, fileName -> exportManager.exportToJson(apps, fileName) }
    }

    /**
     * 导出为CSV并分享
     */
    fun exportAndShareCsv() {
        exportAndShare { apps, fileName -> exportManager.exportToCsv(apps, fileName) }
    }

    /**
     * 导出为TXT并分享
     */
    fun exportAndShareTxt() {
        exportAndShare { apps, fileName -> exportManager.exportToTxt(apps, fileName) }
    }

    /**
     * 通用的导出并分享方法
     */
    private fun exportAndShare(exportFunction: suspend (List<AppInfo>, String?) -> ExportResult) {
        viewModelScope.launch {
            val appsToExport = _uiState.value.filteredApps

            if (appsToExport.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "没有应用可以导出"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null, error = null)

            try {
                val fileName = "app_list_${_uiState.value.searchQuery.ifBlank { "all" }}_${System.currentTimeMillis()}"
                val result = exportFunction(appsToExport, fileName)

                if (result.success && result.filePath != null && result.mimeType != null) {
                    // 导出成功，立即分享
                    exportManager.shareFile(application, result.filePath, result.mimeType)
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = "成功导出并分享 ${result.exportedCount} 个应用"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = if (result.success) {
                            "成功导出 ${result.exportedCount} 个应用到:\n${result.filePath}"
                        } else {
                            "导出失败: ${result.error}"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "导出过程中发生错误: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}