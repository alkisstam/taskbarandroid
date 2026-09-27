package com.alkisstam.taskbar.ui.theme

import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alkisstam.taskbar.service.semSetBlur
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

// Set by OverlayService. Shared by every overlay window so panels don't each need the settings
// threaded through as parameters.
object GlassBlur {
    var available by mutableStateOf(false)
    var radiusPx by mutableIntStateOf(0)
    var tintOverBlur by mutableStateOf(false)

    fun activeFor(enabled: Boolean) = enabled && available && radiusPx > 0
}

// True inside a transparent panel, so its inner backgrounds can go see-through too.
val LocalGlassSurface = compositionLocalOf { false }

// Neutral container backgrounds at half opacity on glass (matches the notification cards).
@Composable
fun Color.glass(): Color = if (LocalGlassSurface.current) copy(alpha = alpha * 0.5f) else this

// Frosts what's behind this panel via Samsung's per-view blur, which draws opaquely over the
// panel's own surface colour; [tint] is re-drawn on top only when the user asks for it.
@Composable
fun GlassBackdrop(enabled: Boolean, cornerRadius: Dp, tint: Color, content: @Composable () -> Unit) {
    val cornerPx = with(LocalDensity.current) { cornerRadius.toPx() }
    Box(propagateMinConstraints = true) {
        if (GlassBlur.activeFor(enabled)) {
            AndroidView(
                factory = { View(it) },
                modifier = Modifier.matchParentSize(),
                update = { it.semSetBlur(GlassBlur.radiusPx, cornerPx) },
                onRelease = { it.semSetBlur(0, 0f) }
            )
            if (GlassBlur.tintOverBlur) Box(Modifier.matchParentSize().background(tint))
        }
        CompositionLocalProvider(LocalGlassSurface provides enabled) { content() }
    }
}
