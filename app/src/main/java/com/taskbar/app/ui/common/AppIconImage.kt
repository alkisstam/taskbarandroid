package com.taskbar.app.ui.common

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIconImage(
    icon: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = produceState<ImageBitmap?>(initialValue = null, key1 = icon) {
        value = withContext(Dispatchers.Default) { icon.toBitmap().asImageBitmap() }
    }
    bitmap.value?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}
