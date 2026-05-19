package com.alkisstam.taskbar.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun AppIconImage(
    icon: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = icon.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
fun AppIconPlaceholder(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
