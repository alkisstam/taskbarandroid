package com.alkisstam.taskbar.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private val Context.clipboardDataStore: DataStore<Preferences> by preferencesDataStore(name = "clipboard_prefs")

enum class ClipType { TEXT, IMAGE, PDF, URL, TEXT_FILE, DOCUMENT }

data class ClipItem(
    val id: String,
    val type: ClipType,
    val content: String,
    val sourceApp: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val fileName: String? = null
)

data class NoteItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false
)

data class TodoItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isDone: Boolean = false
)

@Singleton
class ClipboardRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val CLIPS_KEY = stringPreferencesKey("clips")
        private val NOTES_LIST_KEY = stringPreferencesKey("notes_list")
        private val TODOS_LIST_KEY = stringPreferencesKey("todos_list")
        private val SHARE_HINT_DISMISSED_KEY = booleanPreferencesKey("share_hint_dismissed")
        private const val MAX_CLIPS = 100
        private const val MAX_FILE_BYTES = 50L * 1024 * 1024
    }

    private val clipDir: File
        get() = File(context.filesDir, "clipboard").also { it.mkdirs() }

    private val safeData: Flow<Preferences> = context.clipboardDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val clips: Flow<List<ClipItem>> = safeData.map { prefs ->
        prefs[CLIPS_KEY]?.let { deserializeClips(it) } ?: emptyList()
    }

    val noteItems: Flow<List<NoteItem>> = safeData.map { prefs ->
        prefs[NOTES_LIST_KEY]?.let { deserializeNotes(it) } ?: emptyList()
    }

    val todoItems: Flow<List<TodoItem>> = safeData.map { prefs ->
        prefs[TODOS_LIST_KEY]?.let { deserializeTodos(it) } ?: emptyList()
    }

    val shareHintDismissed: Flow<Boolean> = safeData.map { prefs ->
        prefs[SHARE_HINT_DISMISSED_KEY] ?: false
    }

    suspend fun dismissShareHint() {
        context.clipboardDataStore.edit { prefs ->
            prefs[SHARE_HINT_DISMISSED_KEY] = true
        }
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

    suspend fun addTodo(item: TodoItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[TODOS_LIST_KEY]?.let { deserializeTodos(it) } ?: emptyList()
            prefs[TODOS_LIST_KEY] = serializeTodos(listOf(item) + current)
        }
    }

    suspend fun removeTodo(id: String) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[TODOS_LIST_KEY]?.let { deserializeTodos(it) } ?: emptyList()
            prefs[TODOS_LIST_KEY] = serializeTodos(current.filter { it.id != id })
        }
    }

    suspend fun updateTodo(item: TodoItem) {
        context.clipboardDataStore.edit { prefs ->
            val current = prefs[TODOS_LIST_KEY]?.let { deserializeTodos(it) } ?: emptyList()
            prefs[TODOS_LIST_KEY] = serializeTodos(current.map { if (it.id == item.id) item else it })
        }
    }

    fun copyStreamToStorage(input: InputStream, extension: String, id: String): String {
        val file = File(clipDir, "$id.$extension")
        try {
            file.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    // Uncapped, a mislabeled multi-GB share would silently fill internal storage.
                    if (total > MAX_FILE_BYTES) throw IOException("Shared file exceeds $MAX_FILE_BYTES bytes")
                    out.write(buffer, 0, n)
                }
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return file.absolutePath
    }

    private fun pruneFile(item: ClipItem) {
        if (item.type == ClipType.IMAGE || item.type == ClipType.PDF || item.type == ClipType.DOCUMENT) {
            File(item.content).takeIf { it.exists() }?.delete()
        }
    }

    internal fun serializeClips(clips: List<ClipItem>): String {
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
                put("fileName", item.fileName)
            })
        }
        return arr.toString()
    }

    internal fun deserializeClips(json: String): List<ClipItem> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                ClipItem(
                    id = obj.getString("id"),
                    type = ClipType.valueOf(obj.getString("type")),
                    content = obj.getString("content"),
                    sourceApp = obj.getString("sourceApp"),
                    timestamp = obj.getLong("timestamp"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    fileName = if (obj.has("fileName")) obj.getString("fileName") else null
                )
            } catch (e: Exception) {
                Log.w("ClipboardRepository", "Skipping corrupt clip record at index $i", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.w("ClipboardRepository", "Failed to parse clips JSON", e)
        emptyList()
    }

    internal fun serializeNotes(notes: List<NoteItem>): String {
        val arr = JSONArray()
        notes.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("content", item.content)
                put("timestamp", item.timestamp)
                put("isPinned", item.isPinned)
                put("isFavorite", item.isFavorite)
            })
        }
        return arr.toString()
    }

    internal fun deserializeNotes(json: String): List<NoteItem> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                NoteItem(
                    id = obj.getString("id"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
            } catch (e: Exception) {
                Log.w("ClipboardRepository", "Skipping corrupt note record at index $i", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.w("ClipboardRepository", "Failed to parse notes JSON", e)
        emptyList()
    }

    internal fun serializeTodos(todos: List<TodoItem>): String {
        val arr = JSONArray()
        todos.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("content", item.content)
                put("timestamp", item.timestamp)
                put("isDone", item.isDone)
            })
        }
        return arr.toString()
    }

    internal fun deserializeTodos(json: String): List<TodoItem> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                TodoItem(
                    id = obj.getString("id"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    isDone = obj.optBoolean("isDone", false)
                )
            } catch (e: Exception) {
                Log.w("ClipboardRepository", "Skipping corrupt todo record at index $i", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.w("ClipboardRepository", "Failed to parse todos JSON", e)
        emptyList()
    }
}
