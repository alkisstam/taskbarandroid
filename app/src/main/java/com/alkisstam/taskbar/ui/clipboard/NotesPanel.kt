package com.alkisstam.taskbar.ui.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.NoteItem
import com.alkisstam.taskbar.data.TodoItem
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.glassSheen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
import kotlinx.coroutines.launch

@Composable
fun NotesPanel(
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
    val noteItems by viewModel.noteItems.collectAsState()
    val todoItems by viewModel.todoItems.collectAsState()

    val tabs = listOf(stringResource(R.string.notes_panel_tab_notes), stringResource(R.string.notes_panel_tab_todos))
    val tabIcons = listOf(Icons.Default.Edit, Icons.Default.CheckCircle)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val panelShape = RoundedCornerShape(24.dp)
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface

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
                .clip(panelShape)
                .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha)
                .glassSheen(enabled = translucentMode && !panelOutlineEnabled, shape = panelShape),
            shape = panelShape,
            color = if (translucentMode) panelColor.copy(alpha = translucentAlpha) else panelColor,
            tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> NotesTab(
                                noteItems = noteItems.sortedWith(
                                    compareByDescending<NoteItem> { it.isPinned }.thenByDescending { it.timestamp }
                                ),
                                onAdd = viewModel::addNote,
                                onTogglePin = viewModel::toggleNotePin,
                                onDelete = viewModel::removeNote,
                                onEdit = viewModel::updateNote,
                                onOpenExternal = onOpenExternal
                            )
                            1 -> ToDoTab(
                                todoItems = todoItems,
                                onAdd = viewModel::addTodo,
                                onToggleDone = viewModel::toggleTodoDone,
                                onTogglePin = viewModel::toggleTodoPin,
                                onDelete = viewModel::removeTodo,
                                onEdit = viewModel::updateTodo
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
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
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                        shape = RoundedCornerShape(28.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                tabIcons[index],
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.labelMedium,
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
private fun NotesTab(
    noteItems: List<NoteItem>,
    onAdd: (String) -> Unit,
    onTogglePin: (NoteItem) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (NoteItem) -> Unit,
    onOpenExternal: () -> Unit
) {
    var composerVisible by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    // Distance from this tab's bottom edge to the window (screen) bottom — tab bar, panel
    // padding, and the dock gap all included, so the lift lands the composer on the keyboard.
    var bottomGapPx by remember { mutableIntStateOf(0) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val composerLift = with(density) { (imeBottomPx - bottomGapPx).coerceAtLeast(0).toDp() }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coords ->
            val root = coords.findRootCoordinates()
            bottomGapPx = (root.size.height - (coords.positionInRoot().y + coords.size.height)).toInt().coerceAtLeast(0)
        }
    ) {
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
                        stringResource(R.string.notes_panel_empty_notes),
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
                    contentDescription = if (editingNote != null) stringResource(R.string.notes_panel_fab_cancel_action) else stringResource(R.string.notes_panel_new_note_action),
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
    onTogglePin: (TodoItem) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (TodoItem) -> Unit
) {
    var composerVisible by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    val density = LocalDensity.current
    var bottomGapPx by remember { mutableIntStateOf(0) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val composerLift = with(density) { (imeBottomPx - bottomGapPx).coerceAtLeast(0).toDp() }

    val open = todoItems.filter { !it.isDone }
        .sortedWith(compareByDescending<TodoItem> { it.isPinned }.thenByDescending { it.timestamp })
    val completed = todoItems.filter { it.isDone }.sortedByDescending { it.timestamp }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coords ->
            val root = coords.findRootCoordinates()
            bottomGapPx = (root.size.height - (coords.positionInRoot().y + coords.size.height)).toInt().coerceAtLeast(0)
        }
    ) {
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
                        stringResource(R.string.notes_panel_empty_todos),
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
                        onTogglePin = { onTogglePin(todo) },
                        onDelete = { onDelete(todo.id) }
                    )
                }
                if (completed.isNotEmpty()) {
                    item("completed_header") {
                        Text(
                            stringResource(R.string.notes_panel_completed_count, completed.size),
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
                        onTogglePin = { onTogglePin(todo) },
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
                    placeholder = stringResource(R.string.notes_panel_todo_placeholder),
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
                    contentDescription = if (editingTodo != null) stringResource(R.string.notes_panel_fab_cancel_action) else stringResource(R.string.notes_panel_new_todo_action),
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
    onTogglePin: () -> Unit,
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
            onTogglePin = onTogglePin,
            onDelete = onDelete
        )
    }
}
