package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppSortOrder
import com.alkisstam.taskbar.ui.theme.GlassBackdrop
import com.alkisstam.taskbar.ui.theme.LocalGlassSurface
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.theme.GlassBlur
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.graphics.Color

@Composable
fun AppSortMenuButton(
    currentOrder: AppSortOrder,
    onSelect: (AppSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.app_menu_sort_button_content_description))
        }
        val glassMenu = GlassBlur.activeFor(LocalGlassSurface.current)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (glassMenu) Color.Transparent else MenuDefaults.containerColor,
            tonalElevation = if (glassMenu) 0.dp else MenuDefaults.TonalElevation,
            shadowElevation = if (glassMenu) 0.dp else MenuDefaults.ShadowElevation
        ) {
            // The menu is its own popup window; frost it like the panel it opens from. Its Surface
            // goes fully transparent on glass: the blur can't reach the 8dp padding it adds.
            GlassBackdrop(
                enabled = LocalGlassSurface.current,
                cornerRadius = 12.dp,
                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
            ) {
                Column {
                    val options = listOf(
                        AppSortOrder.NAME to R.string.app_sort_order_name,
                        AppSortOrder.INSTALL_TIME to R.string.app_sort_order_install_time,
                        AppSortOrder.USAGE to R.string.app_sort_order_usage
                    )
                    options.forEach { (order, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            leadingIcon = if (order == currentOrder) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                onSelect(order)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
