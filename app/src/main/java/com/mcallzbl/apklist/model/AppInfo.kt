package com.mcallzbl.apklist.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable? = null,
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val isSystemApp: Boolean = false
)