package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.ui.common.MorphShape
import com.alkisstam.taskbar.ui.common.buttonShape
import com.alkisstam.taskbar.ui.common.pressMorph
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.ui.theme.glass

@Composable
fun AppMenuButton(
    menuOpen: Boolean,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.GridView,
    contentDescription: String = stringResource(R.string.taskbar_app_menu_content_desc),
    iconShape: IconShape = IconShape.CIRCLE
) {
    val morph = remember(iconShape) { iconShape.buttonShape().pressMorph()!! }
    val progress by animateFloatAsState(
        targetValue = if (menuOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "menuButtonMorph"
    )
    val shape = MorphShape(morph, progress)
    Surface(
        modifier = modifier
            .size(size)
            .clip(shape)
            .clickable(onClick = onClick),
        color = if (menuOpen)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.glass(),
        shape = shape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = progress * 45f },
                tint = if (menuOpen)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
