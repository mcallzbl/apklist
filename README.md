# 应用列表导出工具

一个功能强大的Android应用，可以一键导出手机中已安装应用的完整列表。

## 功能特性

### 🔍 应用管理
- **自动扫描**: 自动获取手机中所有已安装的应用
- **智能过滤**: 支持搜索应用名称或包名
- **系统应用识别**: 区分用户应用和系统应用
- **详细信息显示**: 包含应用名称、包名、版本号、图标等信息

### 📤 多格式导出
- **JSON格式**: 结构化数据，包含完整的元数据信息
- **CSV格式**: 表格数据，可用Excel等软件打开分析
- **TXT格式**: 纯文本格式，易于阅读和打印

### 🎨 现代化界面
- **Material 3设计**: 遵循最新的Android设计规范
- **Jetpack Compose**: 流畅的现代化UI体验
- **深色/浅色主题**: 自动适配系统主题

### ⚡ 性能优化
- **异步加载**: 应用列表加载不阻塞UI
- **智能缓存**: 避免重复数据获取
- **内存优化**: 大量应用列表的流畅滚动

## 技术栈

- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM + Repository Pattern
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines + Flow
- **图片加载**: Coil
- **数据导出**: 自定义导出引擎

## 权限说明

应用需要以下权限：

- `QUERY_ALL_PACKAGES`: 查询已安装应用列表
- `WRITE_EXTERNAL_STORAGE`: 导出文件到外部存储 (Android 12及以下)
- `READ_EXTERNAL_STORAGE`: 读取外部存储 (Android 12及以下)

## 使用方法

1. **安装应用**: 下载并安装APK文件
2. **授予权限**: 首次启动时授予必要的权限
3. **浏览应用**: 查看手机中已安装的应用列表
4. **搜索应用**: 使用搜索框快速找到目标应用
5. **导出列表**: 点击导出按钮选择格式导出应用列表

## 导出文件格式

### JSON格式示例
```json
{
  "exportTime": "2024-01-01 12:00:00",
  "totalApps": 100,
  "apps": [
    {
      "appName": "微信",
      "packageName": "com.tencent.mm",
      "versionName": "8.0.28",
      "versionCode": 2680,
      "isSystemApp": false,
      "installTime": 1672531200000,
      "updateTime": 1704067200000
    }
  ]
}
```

### CSV格式示例
```csv
应用名称,包名,版本号,版本代码,系统应用,安装时间,更新时间
微信,com.tencent.mm,8.0.28,2680,false,2023-01-01 12:00,2024-01-01 12:00
```

### TXT格式示例
```
应用列表导出
导出时间: 2024-01-01 12:00:00
应用总数: 100
================================================================================

1. 微信
   包名: com.tencent.mm
   版本: 8.0.28 (2680)
   类型: 用户应用
   安装时间: 2023-01-01 12:00
   更新时间: 2024-01-01 12:00
   ----------------------------------------
```

## 文件位置

导出的文件默认保存在：`存储/Download/ApkList/` 目录下

## 编译和运行

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 11 或更高版本
- Android SDK API 26-36

### 编译步骤
```bash
# 克隆项目
git clone [repository-url]
cd apklist

# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease
```

### 安装到设备
```bash
# 安装Debug版本
adb install app/build/outputs/apk/debug/app-debug.apk

# 安装Release版本
adb install app/build/outputs/apk/release/app-release.apk
```

## 项目结构

```
app/src/main/java/com/mcallzbl/apklist/
├── model/                  # 数据模型
│   └── AppInfo.kt         # 应用信息数据类
├── utils/                  # 工具类
│   ├── AppManager.kt      # 应用管理器
│   ├── ExportManager.kt   # 导出管理器
│   └── PermissionManager.kt # 权限管理器
├── ui/                     # UI组件
│   ├── components/        # 可复用组件
│   │   └── AppListItem.kt # 应用列表项
│   ├── screens/           # 页面
│   │   └── AppListScreen.kt # 主页面
│   └── theme/             # 主题样式
├── viewModel/              # ViewModel
│   └── AppListViewModel.kt # 应用列表ViewModel
├── di/                     # 依赖注入
│   └── AppModule.kt       # Hilt模块
├── base/                   # 基础类
├── MainActivity.kt         # 主Activity
└── App.kt                  # Application类
```

## 注意事项

1. **权限**: 应用需要适当的权限才能获取应用列表和导出文件
2. **存储空间**: 确保设备有足够的存储空间保存导出文件
3. **兼容性**: 支持Android 8.0 (API 26) 及以上版本
4. **性能**: 在设备安装大量应用时，首次加载可能需要一些时间

## 开发计划

- [ ] 支持应用图标导出
- [ ] 添加应用分类功能
- [ ] 支持批量操作
- [ ] 添加导出历史记录
- [ ] 支持自定义导出字段
- [ ] 添加应用启动功能

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 贡献

欢迎提交问题报告和功能请求！如果您想贡献代码，请：

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue: [GitHub Issues](https://github.com/your-username/apklist/issues)
- 邮箱: your-email@example.com

---

**免责声明**: 本应用仅用于个人用途，请遵守相关法律法规，不要用于商业用途或非法用途。