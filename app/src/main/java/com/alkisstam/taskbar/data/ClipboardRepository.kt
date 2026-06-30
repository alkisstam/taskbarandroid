package com.alkisstam.taskbar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private val Context.clipboardDataStore: DataStore<Preferences> by preferencesDataStore(name = "clipboard_prefs")

enum class ClipType { TEXT, IMAGE, PDF, URL }

data class ClipItem(
    val id: String,
    val type: ClipType,
    val content: String,
    val sourceApp: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false
)

data class NoteItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)

@Singleton
class ClipboardRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val CLIPS_KEY = stringPreferencesKey("clips")
        private val NOTES_LIST_KEY = stringPreferencesKey("notes_list")
        private const val MAX_CLIPS = 100
    }

    private val clipDir: File
        get() = File(context.filesDir, "clipboard").also { it.mkdirs() }

    val clips: Flow<List<ClipItem>> = context.clipboardDataStore.data.map { prefs ->
        prefs[CLIPS_KEY]?.let { deserializeClips(it) } ?: emptyList()
    }

    val noteItems: Flow<List<NoteItem>> = context.clipboardDataStore.data.map { prefs ->
        prefs[NOTES_LIST_KEY]?.let { deserializeNotes(it) } ?: emptyList()
    }

    suspend fun addClip(item: ClipItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[CLIPS_KEY]?.let { deserializeClips(it) } ?: emptyList()
            val updated = (listOf(item) + current).let { list ->
                if (list.size <= MAX_CLIPS) return@let list
                val pinned = list.filter { it.isPinned }
                val unpinned = list.filter { !it.isPinned }
                val keep = unpinned.take((MAX_CLIPS - pinned.size).coerceAtLeast(0))
                (unpinned - keep.toSet()).forEach { pruneFile(it) }
                pinned + keep
            }
            prefs[CLIPS_KEY] = serializeClips(updated)
        }
    }

    suspend fun removeClip(id: String) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[CLIPS_KEY]?.let { deserializeClips(it) } ?: emptyList()
            current.find { it.id == id }?.let { pruneFile(it) }
            prefs[CLIPS_KEY] = serializeClips(current.filter { it.id != id })
        }
    }

    suspend fun updateClip(item: ClipItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[CLIPS_KEY]?.let { deserializeClips(it) } ?: emptyList()
            prefs[CLIPS_KEY] = serializeClips(current.map { if (it.id == item.id) item else it })
        }
    }

    suspend fun addNote(item: NoteItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[NOTES_LIST_KEY]?.let { deserializeNotes(it) } ?: emptyList()
            prefs[NOTES_LIST_KEY] = serializeNotes(listOf(item) + current)
        }
    }

    suspend fun removeNote(id: String) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[NOTES_LIST_KEY]?.let { deserializeNotes(it) } ?: emptyList()
            prefs[NOTES_LIST_KEY] = serializeNotes(current.filter { it.id != id })
        }
    }

    suspend fun updateNote(item: NoteItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[NOTES_LIST_KEY]?.let { deserializeNotes(it) } ?: emptyList()
            prefs[NOTES_LIST_KEY] = serializeNotes(current.map { if (it.id == item.id) item else it })
        }
    }

    fun copyStreamToStorage(input: InputStream, extension: String, id: String): String {
        val file = File(clipDir, "$id.$extension")
        file.outputStream().use { input.copyTo(it) }
        return file.absolutePath
    }

    private fun pruneFile(item: ClipItem) {
        if (item.type == ClipType.IMAGE || item.type == ClipType.PDF) {
            File(item.content).takeIf { it.exists() }?.delete()
        }
    }

    private fun serializeClips(clips: List<ClipItem>): String {
        val arr = JSONArray()
        clips.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("type", item.type.name)
                put("content", item.content)
                put("sourceApp", item.sourceApp)
                put("timestamp", item.timestamp)
                put("isPinned", item.isPinned)
                put("isFavorite", item.isFavorite)
            })
        }
        return arr.toString()
    }

    private fun deserializeClips(json: String): List<ClipItem> = try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            ClipItem(
                id = obj.getString("id"),
                type = ClipType.valueOf(obj.getString("type")),
                content = obj.getString("content"),
                sourceApp = obj.getString("sourceApp"),
                timestamp = obj.getLong("timestamp"),
                isPinned = obj.optBoolean("isPinned", false),
                isFavorite = obj.optBoolean("isFavorite", false)
            )
        }
    } catch (e: Exception) { emptyList() }

    private fun serializeNotes(notes: List<NoteItem>): String {
        val arr = JSONArray()
        notes.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("content", item.content)
                put("timestamp", item.timestamp)
                put("isPinned", item.isPinned)
            })
        }
        return arr.toString()
    }

    private fun deserializeNotes(json: String): List<NoteItem> = try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            NoteItem(
                id = obj.getString("id"),
                content = obj.getString("content"),
                timestamp = obj.getLong("timestamp"),
                isPinned = obj.optBoolean("isPinned", false)
            )
        }
    } catch (e: Exception) { emptyList() }
}
