package com.taskbar.app.data

import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
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

private const val TAG = "QuickControlsRepository"

interface QuickControlsChangeListener {
    fun onQuickControlsChanged()
}

@Singleton
class QuickControlsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val changeListeners = mutableListOf<QuickControlsChangeListener>()

    fun addChangeListener(listener: QuickControlsChangeListener) { changeListeners.add(listener) }
    fun removeChangeListener(listener: QuickControlsChangeListener) { changeListeners.remove(listener) }
    private fun notifyChanged() { changeListeners.forEach { it.onQuickControlsChanged() } }

    private val ringerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
            notifyChanged()
        }
    }

    private val settingsObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { notifyChanged() }
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var torchCameraId: String? = null
    @Volatile private var torchState: Boolean = false

    init {
        torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        torchCameraId?.let {
            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchCameraId) torchState = enabled
                    notifyChanged()
                }
            }, null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                ringerReceiver,
                android.content.IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                ringerReceiver,
                android.content.IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
            )
        }
        context.contentResolver.registerContentObserver(
            android.provider.Settings.System.getUriFor(android.provider.Settings.System.ACCELEROMETER_ROTATION),
            false, settingsObserver
        )
        context.contentResolver.registerContentObserver(
            android.provider.Settings.System.getUriFor(android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE),
            false, settingsObserver
        )
    }

    fun hasTorch(): Boolean = torchCameraId != null

    fun getTorchState(): Boolean = torchState

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
            Log.w(TAG, "Permission denied when setting ringer mode", e)
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

    fun getBrightness(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    fun setBrightness(value: Int) {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value.coerceIn(1, 255)
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
        val candidates = buildList {
            // Oppo / ColorOS
            add(Intent("coloros.intent.action.CAMERA_SCANNER"))
            add(Intent("coloros.intent.action.SCANNER_MAIN_PAGE"))
            // Samsung (One UI — opens camera in QR mode)
            add(Intent("com.samsung.android.scanner.SCAN_QR_CODE"))
            add(Intent("com.sec.android.app.camera.BARCODE_SCANNER"))
            // Xiaomi / MIUI
            add(Intent("com.xiaomi.scanner.action.SCAN"))
            // Huawei
            add(Intent("com.huawei.scanner.action.SCAN_AND_RESULT"))
            // Standard Android 9+ (API 28+) — works on Pixel and other AOSP-based devices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                add(Intent("android.media.action.SCAN_BARCODE"))
            }
            // ZXing standalone app (user-installed)
            add(Intent("com.google.zxing.client.android.SCAN").apply {
                putExtra("SCAN_MODE", "QR_CODE_MODE")
            })
            // Google Lens as a last resort (widely available)
            add(Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.google.ar.lens")
            })
        }
        val pm = context.packageManager
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pm.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                return
            }
        }
        Log.w(TAG, "No QR scanner app found on this device")
    }

    fun showPowerMenu(): Boolean {
        val instance = TaskBarAccessibilityService.instance
        return if (instance != null) {
            instance.showPowerMenu()
            true
        } else {
            Log.w(TAG, "Cannot show power menu - accessibility service not running")
            false
        }
    }

    fun canShowPowerMenu(): Boolean = TaskBarAccessibilityService.isRunning()

    fun cleanup() {
        try {
            context.unregisterReceiver(ringerReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    protected fun finalize() {
        cleanup()
    }
}
