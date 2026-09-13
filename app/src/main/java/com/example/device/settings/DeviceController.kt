package com.example.device.settings

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DeviceController(private val context: Context) {

    private var isTorchOn = false

    fun toggleFlashlight(enable: Boolean? = null): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return "Flashlight not supported on this device"
            val targetState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            if (isTorchOn) "Flashlight turned ON" else "Flashlight turned OFF"
        } catch (e: Exception) {
            Log.e("DeviceController", "Flashlight error", e)
            "Could not toggle flashlight: ${e.message}"
        }
    }

    fun openVolumeSettings(): String {
        return try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened sound & volume settings"
        } catch (e: Exception) {
            "Failed to open volume settings"
        }
    }

    fun openWifiSettings(): String {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened Wi-Fi settings"
        } catch (e: Exception) {
            "Failed to open Wi-Fi settings"
        }
    }

    fun openBatterySettings(): String {
        return try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened battery usage settings"
        } catch (e: Exception) {
            "Failed to open battery settings"
        }
    }

    fun getDeviceStatus(): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a, EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentTime = timeFormat.format(now)
        val timeZone = TimeZone.getDefault().displayName

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        return "Current time: $currentTime ($timeZone). Battery level: $batteryPct%."
    }

    fun openWebsite(url: String): String {
        return try {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened $formattedUrl in browser"
        } catch (e: Exception) {
            "Failed to open website: ${e.message}"
        }
    }

    fun searchWeb(query: String): String {
        return try {
            val searchUrl = "https://www.google.com/search?q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Searching web for '$query'"
        } catch (e: Exception) {
            "Failed to search web: ${e.message}"
        }
    }
}
