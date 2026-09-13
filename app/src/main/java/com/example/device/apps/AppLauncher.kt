package com.example.device.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class AppLauncher(private val context: Context) {

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "camera" to "camera_intent",
        "settings" to "settings_intent",
        "calculator" to "calculator_intent",
        "clock" to "clock_intent",
        "play store" to "com.android.vending",
        "gmail" to "com.google.android.gm",
        "whatsapp" to "com.whatsapp",
        "spotify" to "com.spotify.music"
    )

    fun launchApp(appName: String): AppLaunchResult {
        val query = appName.trim().lowercase()
        val packageManager: PackageManager = context.packageManager

        // 1. Check explicit standard intent handlers first
        when (query) {
            "camera" -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(cameraIntent)
                    return AppLaunchResult.Success("Opened Camera")
                }
            }
            "settings", "system settings" -> {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return AppLaunchResult.Success("Opened System Settings")
            }
        }

        // 2. Check well-known package name map
        val mappedPackage = commonPackageMap[query]
        if (mappedPackage != null && !mappedPackage.endsWith("_intent")) {
            val launchIntent = packageManager.getLaunchIntentForPackage(mappedPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return AppLaunchResult.Success("Launched $appName")
            }
        }

        // 3. Search installed applications by label
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val label = packageManager.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                        return AppLaunchResult.Success("Launched $appLabel")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppLauncher", "Error querying installed applications", e)
        }

        // 4. Fallback: Search in Play Store or Web
        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
            AppLaunchResult.Fallback("App not installed. Opening Google Play Store for $appName")
        } catch (e: Exception) {
            AppLaunchResult.NotFound("Application '$appName' is not installed on this device.")
        }
    }
}

sealed class AppLaunchResult {
    data class Success(val message: String) : AppLaunchResult()
    data class Fallback(val message: String) : AppLaunchResult()
    data class NotFound(val message: String) : AppLaunchResult()
}
