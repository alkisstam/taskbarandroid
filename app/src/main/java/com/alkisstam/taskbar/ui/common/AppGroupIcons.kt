package com.alkisstam.taskbar.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

// Keys are persisted; never rename one.
val AppGroupIcons: List<Pair<String, ImageVector>> = listOf(
    "folder" to Icons.Filled.Folder,
    "tools" to Icons.Filled.Build,
    "social" to Icons.Filled.People,
    "chat" to Icons.AutoMirrored.Filled.Chat,
    "productivity" to Icons.Filled.Checklist,
    "work" to Icons.Filled.Work,
    "media" to Icons.Filled.PlayCircle,
    "music" to Icons.Filled.LibraryMusic,
    "photos" to Icons.Filled.PhotoLibrary,
    "games" to Icons.Filled.SportsEsports,
    "shopping" to Icons.Filled.ShoppingCart,
    "finance" to Icons.Filled.AccountBalance,
    "travel" to Icons.Filled.Flight,
    "health" to Icons.Filled.Favorite,
    "education" to Icons.Filled.School,
    "news" to Icons.Filled.Newspaper,
    "star" to Icons.Filled.Star
)

fun appGroupIcon(key: String): ImageVector =
    AppGroupIcons.firstOrNull { it.first == key }?.second ?: Icons.Filled.Folder
