package com.alkisstam.taskbar.service

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
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

    companion object {
        private val OFFICE_DOC_EXTENSIONS = mapOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
            "application/msword" to "doc",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx",
            "application/vnd.ms-excel" to "xls",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx",
            "application/vnd.ms-powerpoint" to "ppt"
        )
    }

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
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text != null) {
                    val type = if (text.startsWith("http://") || text.startsWith("https://")) ClipType.URL else ClipType.TEXT
                    ClipItem(id = id, type = type, content = text, sourceApp = sourceApp, timestamp = timestamp)
                } else {
                    val uri = intentStream() ?: return null
                    val path = try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            clipboardRepository.copyStreamToStorage(input, "txt", id)
                        }
                    } catch (e: Exception) { null } ?: return null
                    ClipItem(id = id, type = ClipType.TEXT_FILE, content = path, sourceApp = sourceApp, timestamp = timestamp, fileName = resolveFileName(uri))
                }
            }
            mimeType.startsWith("text/") -> {
                val uri = intentStream() ?: return null
                val ext = mimeType.substringAfter("/").substringBefore(";").trim().takeIf { it.isNotBlank() } ?: "txt"
                val path = try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        clipboardRepository.copyStreamToStorage(input, ext, id)
                    }
                } catch (e: Exception) { null } ?: return null
                ClipItem(id = id, type = ClipType.TEXT_FILE, content = path, sourceApp = sourceApp, timestamp = timestamp, fileName = resolveFileName(uri))
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
                ClipItem(id = id, type = ClipType.PDF, content = path, sourceApp = sourceApp, timestamp = timestamp, fileName = resolveFileName(uri))
            }
            OFFICE_DOC_EXTENSIONS.containsKey(mimeType) -> {
                val uri = intentStream() ?: return null
                val ext = OFFICE_DOC_EXTENSIONS.getValue(mimeType)
                val path = try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        clipboardRepository.copyStreamToStorage(input, ext, id)
                    }
                } catch (e: Exception) { null } ?: return null
                ClipItem(id = id, type = ClipType.DOCUMENT, content = path, sourceApp = sourceApp, timestamp = timestamp, fileName = resolveFileName(uri))
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

    private fun resolveFileName(uri: Uri): String? {
        val fromResolver = try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) { null }
        return fromResolver
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
    }

    private fun callerPackageLabel(): String {
        val pkg = referrer?.host ?: return "System"
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) { "System" }
    }
}
