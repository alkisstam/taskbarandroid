package com.alkisstam.taskbar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun TaskBarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun Modifier.glassSheen(enabled: Boolean, shape: Shape): Modifier {
    if (!enabled) return this
    return drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.16f),
                1f to Color.Transparent,
                startY = 0f,
                endY = h * 0.4f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.10f),
                startY = h * 0.8f,
                endY = h
            )
        )
        // Stroke straddles the clip edge, so 2dp width leaves a 1dp inner rim.
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.55f),
                0.5f to Color.White.copy(alpha = 0.08f),
                1f to Color.White.copy(alpha = 0.25f),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun Modifier.grain(enabled: Boolean = true, alpha: Float = 0.10f): Modifier {
    if (!enabled) return this
    return composed {
        val noiseBitmap: ImageBitmap = remember(alpha) {
            val tileSize = 128
            val bmp = android.graphics.Bitmap.createBitmap(tileSize, tileSize, android.graphics.Bitmap.Config.ARGB_8888)
            val pixels = IntArray(tileSize * tileSize)
            val rng = java.util.Random(0xAB1C3D)
            for (i in pixels.indices) {
                val a = (rng.nextFloat() * alpha * 255).toInt()
                val g = rng.nextInt(256)
                pixels[i] = android.graphics.Color.argb(a, g, g, g)
            }
            bmp.setPixels(pixels, 0, tileSize, 0, 0, tileSize, tileSize)
            bmp.asImageBitmap()
        }
        drawWithContent {
            drawContent()
            val bw = noiseBitmap.width.toFloat()
            val bh = noiseBitmap.height.toFloat()
            var ty = 0f
            while (ty < size.height) {
                var tx = 0f
                while (tx < size.width) {
                    drawImage(noiseBitmap, topLeft = Offset(tx, ty))
                    tx += bw
                }
                ty += bh
            }
        }
    }
}
