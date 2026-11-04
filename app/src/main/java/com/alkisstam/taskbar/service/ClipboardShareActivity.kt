package com.alkisstam.taskbar.service

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipType
import com.alkisstam.taskbar.data.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

        internal fun isAllowedStream(uri: Uri, ownPackage: String): Boolean =
            uri.scheme == "content" && uri.authority != "$ownPackage.fileprovider"
    }

    @Inject lateinit var clipboardRepository: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val item = withContext(Dispatchers.IO) { buildClipItem() }
                if (item != null) {
                    withContext(Dispatchers.IO) { clipboardRepository.addClip(item) }
                    sendBroadcast(
                        Intent(OverlayService.ACTION_CLIPBOARD_PANEL_SHOW).apply { setPackage(packageName) }
                    )
                }
            } catch (e: Exception) {
                Log.w("ClipboardShareActivity", "Failed to save shared item", e)
            } finally {
                finish()
            }
        }
    }

    private fun buildClipItem(): ClipItem? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val mimeType = intent.type?.lowercase() ?: return null
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val sourceApp = callerPackageLabel()

        fun fileClip(ext: String, type: ClipType, withName: Boolean = true): ClipItem? {
            val uri = intentStream() ?: return null
            val path = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    clipboardRepository.copyStreamToStorage(input, ext, id)
                }
            } catch (e: Exception) { null } ?: return null
            return ClipItem(id = id, type = type, content = path, sourceApp = sourceApp, timestamp = timestamp,
                fileName = if (withName) resolveFileName(uri) else null)
        }
        val subtype = mimeType.substringAfter("/").substringBefore(";").substringBefore("+").trim()

        return when {
            mimeType == "text/plain" -> {
                // Inline text lives in the DataStore prefs blob; cap it so one giant share
                // can't balloon the file every subsequent read has to load.
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.take(100_000)
                if (text != null) {
                    val type = if (text.startsWith("http://") || text.startsWith("https://")) ClipType.URL else ClipType.TEXT
                    ClipItem(id = id, type = type, content = text, sourceApp = sourceApp, timestamp = timestamp)
                } else fileClip("txt", ClipType.TEXT_FILE)
            }
            mimeType.startsWith("text/") -> fileClip(subtype.ifBlank { "txt" }, ClipType.TEXT_FILE)
            mimeType.startsWith("image/") -> fileClip("img", ClipType.IMAGE, withName = false)?.let { withImageExtension(it, mimeType) }
            mimeType == "application/pdf" -> fileClip("pdf", ClipType.PDF)
            OFFICE_DOC_EXTENSIONS.containsKey(mimeType) -> fileClip(OFFICE_DOC_EXTENSIONS.getValue(mimeType), ClipType.DOCUMENT)
            else -> null
        }
    }

    // FileProvider derives the MIME type from the extension, and senders often mislabel images
    // (a PNG as image/* or image/jpeg), so name the file after the format its bytes actually have.
    private fun withImageExtension(item: ClipItem, declared: String): ClipItem {
        val file = File(item.content)
        val sniffed = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeFile(file.path, it) }.outMimeType
        val provided = intentStream()?.let { runCatching { contentResolver.getType(it) }.getOrNull() }
        val ext = listOfNotNull(sniffed, provided, declared)
            .filter { it.startsWith("image/") }
            .firstNotNullOfOrNull { MimeTypeMap.getSingleton().getExtensionFromMimeType(it.lowercase()) }
            ?: "jpg"
        val renamed = File(file.parentFile, "${file.nameWithoutExtension}.$ext")
        return if (file.renameTo(renamed)) item.copy(content = renamed.absolutePath) else item
    }

    // Exported: a file:// or own-provider URI would let any app make us copy our private files.
    private fun intentStream(): Uri? {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        return uri?.takeIf { isAllowedStream(it, packageName) }
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
