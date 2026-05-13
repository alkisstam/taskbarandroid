package com.taskbar.app.data

import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import com.taskbar.app.service.TaskBarAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickControlsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var torchCameraId: String? = null

    init {
        torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun hasTorch(): Boolean = torchCameraId != null

    fun setTorch(enabled: Boolean) {
        torchCameraId?.let { id ->
            cameraManager.setTorchMode(id, enabled)
        }
    }

    fun getRingerMode(): Int = audioManager.ringerMode

    fun canSetSilent(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun setRingerMode(mode: Int) {
        try {
            audioManager.ringerMode = mode
        } catch (e: SecurityException) {
        }
    }

    fun isAutoRotateEnabled(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1
    }

    fun setAutoRotate(enabled: Boolean) {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0
            )
        }
    }

    fun isAutoBrightnessEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }

    fun setAutoBrightness(enabled: Boolean) {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    private fun notificationManager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun isDndEnabled(): Boolean =
        notificationManager().currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    fun isDndPermissionGranted(): Boolean =
        notificationManager().isNotificationPolicyAccessGranted

    fun toggleDnd() {
        val nm = notificationManager()
        if (!nm.isNotificationPolicyAccessGranted) return
        val next = if (isDndEnabled())
            NotificationManager.INTERRUPTION_FILTER_ALL
        else
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        nm.setInterruptionFilter(next)
    }

    fun openQrScanner() {
        val candidates = listOf(
            Intent("coloros.intent.action.CAMERA_SCANNER"),
            Intent("coloros.intent.action.SCANNER_MAIN_PAGE"),
            Intent("com.google.zxing.client.android.SCAN").apply {
                putExtra("SCAN_MODE", "QR_CODE_MODE")
            },
            Intent("com.google.android.gms.actions.SCAN_BARCODE"),
            Intent("android.media.action.IMAGE_CAPTURE")
        )
        val pm = context.packageManager
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pm.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                return
            }
        }
    }

    fun showPowerMenu() {
        TaskBarAccessibilityService.instance?.showPowerMenu()
    }

    fun canShowPowerMenu(): Boolean = TaskBarAccessibilityService.isRunning()
}
