package com.alkisstam.taskbar.ui.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipType
import com.alkisstam.taskbar.data.NoteItem
import com.alkisstam.taskbar.data.TodoItem
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
import com.alkisstam.taskbar.viewmodel.FavoriteEntry
import kotlinx.coroutines.launch

private enum class ClipCategory(val label: String) {
    ALL("All"), TEXT("Text"), IMAGES("Images"), FILES("Files"), LINKS("Links")
}

private fun ClipCategory.matches(type: ClipType): Boolean = when (this) {
    ClipCategory.ALL -> true
    ClipCategory.TEXT -> type == ClipType.TEXT
    ClipCategory.IMAGES -> type == ClipType.IMAGE
    ClipCategory.FILES -> type == ClipType.PDF || type == ClipType.TEXT_FILE || type == ClipType.DOCUMENT
    ClipCategory.LINKS -> type == ClipType.URL
}

@Composable
fun ClipboardPanel(
    viewModel: ClipboardViewModel,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit = {},
    panelOutlineEnabled: Boolean = false,
    translucentMode: Boolean = false,
    translucentAlpha: Float = 0.80f,
    grainAlpha: Float = 0.10f,
    surfaceTintColor: Long = 0L,
    dockBottomPadding: Dp = 0.dp
) {
    val clips by viewModel.clips.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val noteItems by viewModel.noteItems.collectAsState()
    val todoItems by viewModel.todoItems.collectAsState()
    val shareHintDismissed by viewModel.shareHintDismissed.collectAsState()

    val tabs = listOf("Clips", "Favorites", "Notes", "To-Dos")
    val tabIcons = listOf(Icons.Default.ContentPaste, Icons.Default.Star, Icons.Default.Edit, Icons.Default.CheckCircle)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(ClipCategory.ALL) }
    val density = LocalDensity.current
    var tabBarHeight by remember { mutableStateOf(0.dp) }

    val panelShape = RoundedCornerShape(24.dp)
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 4.dp, start = 2.dp, end = 2.dp)
                .padding(bottom = dockBottomPadding)
                .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, panelShape) else Modifier)
                .then(if (translucentMode && !panelOutlineEnabled) Modifier.border(1.dp, glassBorderColor, panelShape) else Modifier)
                .clip(panelShape)
                .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha),
            shape = panelShape,
            color = if (translucentMode) panelColor.copy(alpha = translucentAlpha) else panelColor,
            tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (pagerState.currentPage == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClipCategory.entries.forEach { category ->
                            val selected = category == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { selectedCategory = category }
                            ) {
                                Text(
                                    category.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> ClipListTab(
                                items = clips
                                    .filter { selectedCategory.matches(it.type) }
                                    .sortedWith(
                                        compareByDescending<ClipItem> { it.isPinned }.thenByDescending { it.timestamp }
                                    ),
                                viewModel = viewModel,
                                onOpenExternal = onOpenExternal,
                                showShareHint = !shareHintDismissed,
                                onDismissShareHint = viewModel::dismissShareHint
                            )
                            1 -> FavoritesTab(
                                entries = favorites,
                                viewModel = viewModel,
                                onOpenExternal = onOpenExternal
                            )
                            2 -> NotesTab(
                                noteItems = noteItems.sortedWith(
                                    compareByDescending<NoteItem> { it.isPinned }.thenByDescending { it.timestamp }
                                ),
                                onAdd = viewModel::addNote,
                                onTogglePin = viewModel::toggleNotePin,
                                onToggleFavorite = viewModel::toggleNoteFavorite,
                                onDelete = viewModel::removeNote,
                                onEdit = viewModel::updateNote,
                                onOpenExternal = onOpenExternal,
                                tabBarHeight = tabBarHeight
                            )
                            3 -> ToDoTab(
                                todoItems = todoItems,
                                onAdd = viewModel::addTodo,
                                onToggleDone = viewModel::toggleTodoDone,
                                onDelete = viewModel::removeTodo,
                                onEdit = viewModel::updateTodo,
                                tabBarHeight = tabBarHeight
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            tabBarHeight = with(density) { coordinates.size.height.toDp() }
                        }
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(40.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Surface(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 56.dp)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                tabIcons[index],
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            tabIcons[index],
                                            contentDescription = title,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipListTab(
    items: List<ClipItem>,
    viewModel: ClipboardViewModel,
    onOpenExternal: () -> Unit,
    showShareHint: Boolean,
    onDismissShareHint: () -> Unit
) {
    var editingClip by remember { mutableStateOf<ClipItem?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (showShareHint) {
            item(key = "share_hint") { ShareHintCard(onDismiss = onDismissShareHint) }
        }
        if (items.isEmpty()) {
            item(key = "empty_state") {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Nothing here yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                if (item.id == editingClip?.id) {
                    NoteComposer(
                        initialText = item.content,
                        onSave = { text ->
                            viewModel.updateClip(item.copy(content = text))
                            editingClip = null
                        },
                        onCancel = { editingClip = null }
                    )
                } else {
                    ClipItemCard(
                        item = item,
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onTogglePin = { viewModel.togglePin(item) },
                        onDelete = { viewModel.removeClip(item.id) },
                        onOpenExternal = onOpenExternal,
                        onEdit = if (item.type == ClipType.TEXT || item.type == ClipType.URL) {
                            { editingClip = item }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareHintCard(onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save from any app",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                "Share text, links, images or files from any app to Floating Dock to save them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FavoritesTab(entries: List<FavoriteEntry>, viewModel: ClipboardViewModel, onOpenExternal: () -> Unit) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Nothing here yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    val context = LocalContext.current
    var editingClip by remember { mutableStateOf<ClipItem?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            entries,
            key = { entry ->
                when (entry) {
                    is FavoriteEntry.Clip -> "clip_${entry.item.id}"
                    is FavoriteEntry.Note -> "note_${entry.item.id}"
                }
            }
        ) { entry ->
            when (entry) {
                is FavoriteEntry.Clip -> if (entry.item.id == editingClip?.id) {
                    NoteComposer(
                        initialText = entry.item.content,
                        onSave = { text ->
                            viewModel.updateClip(entry.item.copy(content = text))
                            editingClip = null
                        },
                        onCancel = { editingClip = null }
                    )
                } else ClipItemCard(
                    item = entry.item,
                    onToggleFavorite = { viewModel.toggleFavorite(entry.item) },
                    onTogglePin = { viewModel.togglePin(entry.item) },
                    onDelete = { viewModel.removeClip(entry.item.id) },
                    onOpenExternal = onOpenExternal,
                    onEdit = if (entry.item.type == ClipType.TEXT || entry.item.type == ClipType.URL) {
                        { editingClip = entry.item }
                    } else null
                )
                is FavoriteEntry.Note -> NoteItemCard(
                    note = entry.item,
                    onCopy = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("note", entry.item.content))
                    },
                    onShare = {
                        onOpenExternal()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, entry.item.content)
                        }
                        context.startActivity(Intent.createChooser(intent, null).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                    onTogglePin = { viewModel.toggleNotePin(entry.item) },
                    onToggleFavorite = { viewModel.toggleNoteFavorite(entry.item) },
                    onDelete = { viewModel.removeNote(entry.item.id) }
                )
            }
        }
    }
}

@Composable
private fun NotesTab(
    noteItems: List<NoteItem>,
    onAdd: (String) -> Unit,
    onTogglePin: (NoteItem) -> Unit,
    onToggleFavorite: (NoteItem) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (NoteItem) -> Unit,
    onOpenExternal: () -> Unit,
    tabBarHeight: Dp = 0.dp
) {
    var composerVisible by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val composerLift = (imeHeight - tabBarHeight).coerceAtLeast(0.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        if (noteItems.isEmpty() && !composerVisible && editingNote == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No notes yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(noteItems, key = { it.id }) { note ->
                    if (note.id == editingNote?.id) {
                        NoteComposer(
                            initialText = note.content,
                            onSave = { text ->
                                onEdit(note.copy(content = text))
                                editingNote = null
                            },
                            onCancel = { editingNote = null }
                        )
                    } else {
                        NoteItemCard(
                            note = note,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("note", note.content))
                            },
                            onShare = {
                                onOpenExternal()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, note.content)
                                }
                                context.startActivity(Intent.createChooser(intent, null).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            onTogglePin = { onTogglePin(note) },
                            onToggleFavorite = { onToggleFavorite(note) },
                            onEdit = {
                                composerVisible = false
                                editingNote = note
                            },
                            onDelete = { onDelete(note.id) }
                        )
                    }
                }
            }
        }

        if (editingNote == null && composerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .offset(y = -composerLift)
            ) {
                NoteComposer(
                    autoFocus = true,
                    onSave = { text -> onAdd(text); composerVisible = false },
                    onCancel = { composerVisible = false }
                )
            }
        }

        if (!composerVisible) {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    composerVisible = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (editingNote != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (editingNote != null) "Cancel" else "New note",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ToDoTab(
    todoItems: List<TodoItem>,
    onAdd: (String) -> Unit,
    onToggleDone: (TodoItem) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (TodoItem) -> Unit,
    tabBarHeight: Dp = 0.dp
) {
    var composerVisible by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val composerLift = (imeHeight - tabBarHeight).coerceAtLeast(0.dp)

    val open = todoItems.filter { !it.isDone }.sortedByDescending { it.timestamp }
    val completed = todoItems.filter { it.isDone }.sortedByDescending { it.timestamp }

    Box(modifier = Modifier.fillMaxSize()) {
        if (todoItems.isEmpty() && !composerVisible && editingTodo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No to-dos yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(open, key = { it.id }) { todo ->
                    TodoListItem(
                        todo = todo,
                        isEditing = todo.id == editingTodo?.id,
                        onStartEdit = { composerVisible = false; editingTodo = todo },
                        onSaveEdit = { text -> onEdit(todo.copy(content = text)); editingTodo = null },
                        onCancelEdit = { editingTodo = null },
                        onToggleDone = { onToggleDone(todo) },
                        onDelete = { onDelete(todo.id) }
                    )
                }
                if (completed.isNotEmpty()) {
                    item("completed_header") {
                        Text(
                            "Completed (${completed.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                items(completed, key = { it.id }) { todo ->
                    TodoListItem(
                        todo = todo,
                        isEditing = todo.id == editingTodo?.id,
                        onStartEdit = { composerVisible = false; editingTodo = todo },
                        onSaveEdit = { text -> onEdit(todo.copy(content = text)); editingTodo = null },
                        onCancelEdit = { editingTodo = null },
                        onToggleDone = { onToggleDone(todo) },
                        onDelete = { onDelete(todo.id) }
                    )
                }
            }
        }

        if (editingTodo == null && composerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .offset(y = -composerLift)
            ) {
                NoteComposer(
                    placeholder = "Add a to-do…",
                    autoFocus = true,
                    onSave = { text -> onAdd(text); composerVisible = false },
                    onCancel = { composerVisible = false }
                )
            }
        }

        if (!composerVisible) {
            FloatingActionButton(
                onClick = {
                    editingTodo = null
                    composerVisible = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (editingTodo != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (editingTodo != null) "Cancel" else "New to-do",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TodoListItem(
    todo: TodoItem,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    if (isEditing) {
        NoteComposer(
            initialText = todo.content,
            placeholder = "Add a to-do…",
            onSave = onSaveEdit,
            onCancel = onCancelEdit
        )
    } else {
        TodoItemCard(
            todo = todo,
            onToggleDone = onToggleDone,
            onEdit = onStartEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun NoteComposer(
    initialText: String = "",
    placeholder: String = "Write a note…",
    autoFocus: Boolean = false,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(placeholder) },
                shape = RoundedCornerShape(8.dp),
                minLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (text.isNotBlank()) onSave(text) }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun NoteItemCard(
    note: NoteItem,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        "Note",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTimestamp(note.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.isPinned) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val lineHeightDp = with(LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .heightIn(max = lineHeightDp * 8)
                    .verticalScroll(rememberScrollState())
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(20.dp),
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onTogglePin) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pin",
                        modifier = Modifier.size(20.dp),
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoItemCard(
    todo: TodoItem,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (todo.isDone) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (todo.isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = todo.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (todo.isDone) 0.6f else 1f),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days d ago"
    }
}
