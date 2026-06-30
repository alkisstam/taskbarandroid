package com.alkisstam.taskbar.service

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipType
import com.alkisstam.taskbar.data.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardShareActivity : ComponentActivity() {

    @Inject lateinit var clipboardRepository: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = buildClipItem()
        if (item != null) {
            CoroutineScope(Dispatchers.IO).launch { clipboardRepository.addClip(item) }
            sendBroadcast(
                Intent(OverlayService.ACTION_CLIPBOARD_PANEL_SHOW).apply { setPackage(packageName) }
            )
        }
        finish()
    }

    private fun buildClipItem(): ClipItem? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val mimeType = intent.type ?: return null
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val sourceApp = callerPackageLabel()

        return when {
            mimeType == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                val type = if (text.startsWith("http://") || text.startsWith("https://")) ClipType.URL else ClipType.TEXT
                ClipItem(id = id, type = type, content = text, sourceApp = sourceApp, timestamp = timestamp)
            }
            mimeType.startsWith("image/") -> {
                val uri = intentStream() ?: return null
                val path = try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        clipboardRepository.copyStreamToStorage(input, "jpg", id)
                    }
                } catch (e: Exception) { null } ?: return null
                ClipItem(id = id, type = ClipType.IMAGE, content = path, sourceApp = sourceApp, timestamp = timestamp)
            }
            mimeType == "application/pdf" -> {
                val uri = intentStream() ?: return null
                val path = try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        clipboardRepository.copyStreamToStorage(input, "pdf", id)
                    }
                } catch (e: Exception) { null } ?: return null
                ClipItem(id = id, type = ClipType.PDF, content = path, sourceApp = sourceApp, timestamp = timestamp)
            }
            else -> null
        }
    }

    private fun intentStream(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
    }

    private fun callerPackageLabel(): String {
        val pkg = referrer?.host ?: return "System"
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) { "System" }
    }
}
