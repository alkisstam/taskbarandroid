package com.alkisstam.taskbar.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.alkisstam.taskbar.service.TaskBarAccessibilityService
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
        torchCameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enumerate cameras for torch", e)
            null
        }
        torchCameraId?.let {
            try {
                cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (cameraId == torchCameraId) torchState = enabled
                        notifyChanged()
                    }
                }, null)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register torch callback", e)
            }
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
        if (!Settings.System.canWrite(context)) return
        if (isAutoBrightnessEnabled()) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value.coerceIn(1, 255)
        )
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    fun getScreenTimeout(): Int =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)

    fun setScreenTimeout(ms: Int) {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
        }
    }

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

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    fun openWifiPanel() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Settings.Panel.ACTION_WIFI
        else
            Settings.ACTION_WIFI_SETTINGS
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open wifi panel", e)
        }
    }

    fun isBluetoothEnabled(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
    }

    fun openBluetoothPanel() {
        // No Settings.Panel entry exists for Bluetooth (only Wifi/NFC/Volume/Internet) -
        // full settings screen is the closest available system UI.
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open bluetooth panel", e)
        }
    }

    fun isMobileDataEnabled(): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return false
        return try {
            tm.isDataEnabled
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun openMobileDataPanel() {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open mobile network settings, falling back to wireless settings", e)
            val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                Settings.Panel.ACTION_INTERNET_CONNECTIVITY
            else
                Settings.ACTION_WIRELESS_SETTINGS
            try {
                context.startActivity(Intent(fallback).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to open fallback wireless settings", e2)
            }
        }
    }

    fun openQuickShare() {
        val pm = context.packageManager
        // Google Play services' own Quick Share / Nearby Share entry point - opens the
        // standalone "Ready to receive files" window with no content attached. Not a
        // documented SDK constant, but present on virtually all GMS devices (verified
        // on-device via `adb shell dumpsys package com.google.android.gms`).
        val gmsQuickShareIntent = Intent("com.google.android.gms.nearby.sharing.UNIFIED").apply {
            setPackage("com.google.android.gms")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (pm.resolveActivity(gmsQuickShareIntent, 0) != null) {
            try {
                context.startActivity(gmsQuickShareIntent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch GMS Quick Share", e)
            }
        }
        val candidates = listOf(
            "com.samsung.android.app.sharelive",
            "com.miui.mishare.connectivity"
        )
        for (pkg in candidates) {
            val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch Quick Share app $pkg", e)
            }
        }
        Log.w(TAG, "No Quick Share app or Nearby Share settings found on this device")
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
                try {
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch QR scanner intent", e)
                }
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

    fun takeScreenshot(): Boolean {
        val instance = TaskBarAccessibilityService.instance
        return if (instance != null) {
            instance.takeScreenshot()
        } else {
            Log.w(TAG, "Cannot take screenshot - accessibility service not running")
            false
        }
    }

    fun canTakeScreenshot(): Boolean = TaskBarAccessibilityService.isRunning()

    fun lockScreen(): Boolean {
        val instance = TaskBarAccessibilityService.instance
        return if (instance != null) {
            instance.lockScreen()
        } else {
            Log.w(TAG, "Cannot lock screen - accessibility service not running")
            false
        }
    }

    fun canLockScreen(): Boolean = TaskBarAccessibilityService.isRunning()

}
