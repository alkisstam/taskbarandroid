package com.alkisstam.taskbar.ui.clipboard

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipType
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
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
    val shareHintDismissed by viewModel.shareHintDismissed.collectAsState()

    val tabs = listOf("Clips", "Favorites")
    val tabIcons = listOf(Icons.Default.ContentPaste, Icons.Default.Star)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(ClipCategory.ALL) }

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
                                items = favorites,
                                viewModel = viewModel,
                                onOpenExternal = onOpenExternal
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
private fun FavoritesTab(items: List<ClipItem>, viewModel: ClipboardViewModel, onOpenExternal: () -> Unit) {
    if (items.isEmpty()) {
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
    var editingClip by remember { mutableStateOf<ClipItem?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
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
