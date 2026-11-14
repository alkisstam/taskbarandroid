package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.alkisstam.taskbar.ui.common.AboveAnchorPopupPosition

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
        if (expanded) Popup(
            popupPositionProvider = AboveAnchorPopupPosition,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 3.dp, shadowElevation = 8.dp) {
                // Popups inherit the panel's glass state, so the menu frosts like the panel it opens from.
                GlassBackdrop(
                    enabled = LocalGlassSurface.current,
                    cornerRadius = 12.dp,
                    tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
                ) {
                    Column(Modifier.width(IntrinsicSize.Max)) {
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
}
