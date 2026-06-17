package com.javis.os.features.device

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceStatus(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val volumePercent: Int,
    val brightnessPercent: Int,
    val isWifiOn: Boolean,
    val isFlashlightOn: Boolean
)

@Singleton
class DeviceControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isFlashlightOn = false

    fun getBatteryStatus(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(percent, isCharging)
    }

    fun formatBatteryResponse(): String {
        val (percent, isCharging) = getBatteryStatus()
        return when {
            percent < 0 -> "I couldn't read the battery level right now."
            isCharging -> "Battery is at $percent% and currently charging."
            percent <= 15 -> "Battery is critically low at $percent%. You should charge soon."
            percent <= 30 -> "Battery is at $percent%. Consider charging soon."
            else -> "Battery is at $percent%."
        }
    }

    fun setVolume(streamType: Int = AudioManager.STREAM_RING, percent: Int) {
        val maxVol = audioManager.getStreamMaxVolume(streamType)
        val targetVol = (maxVol * percent / 100).coerceIn(0, maxVol)
        audioManager.setStreamVolume(streamType, targetVol, AudioManager.FLAG_SHOW_UI)
    }

    fun increaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun decreaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun muteVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun getCurrentVolume(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100 / max) else 0
    }

    fun toggleFlashlight(): Boolean {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            isFlashlightOn
        } catch (e: Exception) {
            false
        }
    }

    fun turnFlashlightOn() {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, true)
            isFlashlightOn = true
        } catch (e: Exception) { /* ignore */ }
    }

    fun turnFlashlightOff() {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, false)
            isFlashlightOn = false
        } catch (e: Exception) { /* ignore */ }
    }

    fun getDeviceStatusSummary(): String {
        val (battery, charging) = getBatteryStatus()
        val volume = getCurrentVolume()
        val chargingStr = if (charging) " (charging)" else ""
        return "Battery: $battery%$chargingStr. Volume: $volume%."
    }

    fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openMobileDataSettings() {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
