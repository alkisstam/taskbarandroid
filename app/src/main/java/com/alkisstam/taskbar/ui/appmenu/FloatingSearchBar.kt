package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingSearchBar(
    viewModel: AppMenuViewModel,
    onHideTaskbar: () -> Unit = {},
    translucentMode: Boolean = false,
    translucentAlpha: Float = 0.80f,
    surfaceTintColor: Long = 0L
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val pinnedPackages by viewModel.pinnedPackages.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val showRecentApps by viewModel.showRecentApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop scrim to dismiss on tap outside
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { viewModel.closeSearch() }
        )
        val surfaceColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
        val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (translucentMode) Modifier.border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)) else Modifier),
                shape = RoundedCornerShape(16.dp),
                color = if (translucentMode) surfaceColor.copy(alpha = translucentAlpha) else surfaceColor,
                tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 8.dp,
                shadowElevation = 8.dp
            ) {
                // Show keyboard reliably when search field is laid out
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search apps…") },
                    leadingIcon = {
                        IconButton(onClick = { viewModel.closeSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    } else {
                        { Icon(Icons.Filled.Search, contentDescription = null) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            // Launch the top search result if any
                            val apps = filteredApps
                            if (apps.isNotEmpty()) {
                                viewModel.launchApp(apps.first().packageName)
                                onHideTaskbar()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (searchQuery.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 360.dp)
                        .then(if (translucentMode) Modifier.border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)) else Modifier),
                    shape = RoundedCornerShape(16.dp),
                    color = if (translucentMode) surfaceColor.copy(alpha = translucentAlpha) else surfaceColor,
                    tonalElevation = if (translucentMode) 0.dp else 6.dp,
                    shadowElevation = 6.dp
                ) {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                            SearchResultItem(
                                app = app,
                                isPinned = pinnedPackages.contains(app.packageName),
                                isHighlighted = index == 0,
                                onLaunch = { viewModel.launchApp(app.packageName); onHideTaskbar() },
                                onPin = {
                                    if (pinnedPackages.contains(app.packageName))
                                        viewModel.unpinApp(app.packageName)
                                    else
                                        viewModel.pinApp(app.packageName)
                                }
                            )
                        }
                    }
                }
            } else if (showRecentApps && recentApps.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 360.dp)
                        .then(if (translucentMode) Modifier.border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)) else Modifier),
                    shape = RoundedCornerShape(16.dp),
                    color = if (translucentMode) surfaceColor.copy(alpha = translucentAlpha) else surfaceColor,
                    tonalElevation = if (translucentMode) 0.dp else 6.dp,
                    shadowElevation = 6.dp
                ) {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(recentApps, key = { it.packageName }) { app ->
                            SearchResultItem(
                                app = app,
                                isPinned = pinnedPackages.contains(app.packageName),
                                onLaunch = { viewModel.launchApp(app.packageName); onHideTaskbar() },
                                onPin = {
                                    if (pinnedPackages.contains(app.packageName))
                                        viewModel.unpinApp(app.packageName)
                                    else
                                        viewModel.pinApp(app.packageName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
