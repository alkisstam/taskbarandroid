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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
