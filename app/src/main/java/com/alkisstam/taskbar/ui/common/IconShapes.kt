@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alkisstam.taskbar.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import com.alkisstam.taskbar.data.IconShape

val SquirclePolygon = RoundedPolygon.rectangle(rounding = CornerRounding(0.5f, smoothing = 1f)).normalized()
val CookiePolygon = MaterialShapes.Cookie9Sided
val CloverPolygon = MaterialShapes.Clover4Leaf
val PebblePolygon = RoundedPolygon(6, rounding = CornerRounding(0.6f, smoothing = 0.6f)).normalized()
val CirclePolygon = MaterialShapes.Circle
private val ArrowPolygon = MaterialShapes.Arrow
private val PixelCirclePolygon = MaterialShapes.PixelCircle
private val SunnyPolygon = MaterialShapes.Sunny
private val SquarePolygon = RoundedPolygon.rectangle().normalized()

// Polygons are normalized to the unit square, so the path only needs scaling to the layout size.
private fun outlineOf(path: android.graphics.Path, size: Size): Outline {
    path.transform(android.graphics.Matrix().apply { setScale(size.width, size.height) })
    return Outline.Generic(path.asComposePath())
}

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        outlineOf(polygon.toPath(), size)
}

class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        outlineOf(morph.toPath(progress), size)
}

val SquircleShape: Shape = PolygonShape(SquirclePolygon)
private val CookieShape: Shape = PolygonShape(CookiePolygon)
private val CloverShape: Shape = PolygonShape(CloverPolygon)
private val PebbleShape: Shape = PolygonShape(PebblePolygon)
private val ArrowShape: Shape = PolygonShape(ArrowPolygon)
private val PixelCircleShape: Shape = PolygonShape(PixelCirclePolygon)
private val SunnyShape: Shape = PolygonShape(SunnyPolygon)

fun IconShape.toComposeShape(): Shape = when (this) {
    IconShape.DEFAULT, IconShape.SQUARE -> RectangleShape
    IconShape.SQUIRCLE -> SquircleShape
    IconShape.CIRCLE -> CircleShape
    IconShape.COOKIE -> CookieShape
    IconShape.CLOVER -> CloverShape
    IconShape.PEBBLE -> PebbleShape
    IconShape.ARROW -> ArrowShape
    IconShape.PIXEL_CIRCLE -> PixelCircleShape
    IconShape.SUNNY -> SunnyShape
}

// Buttons have no shape of their own, so the unmasked/hard-cornered options fall back to soft ones.
fun IconShape.buttonShape(): IconShape = when (this) {
    IconShape.DEFAULT -> IconShape.CIRCLE
    IconShape.SQUARE -> IconShape.SQUIRCLE
    else -> this
}

fun IconShape.pressMorph(): Morph? = when (this) {
    IconShape.DEFAULT -> null
    IconShape.SQUARE -> Morph(SquarePolygon, SquirclePolygon)
    IconShape.SQUIRCLE -> Morph(SquirclePolygon, CookiePolygon)
    IconShape.CIRCLE -> Morph(CirclePolygon, CookiePolygon)
    IconShape.PEBBLE -> Morph(PebblePolygon, CookiePolygon)
    IconShape.COOKIE -> Morph(CookiePolygon, CirclePolygon)
    IconShape.CLOVER -> Morph(CloverPolygon, CirclePolygon)
    IconShape.ARROW -> Morph(ArrowPolygon, CirclePolygon)
    IconShape.PIXEL_CIRCLE -> Morph(PixelCirclePolygon, CirclePolygon)
    IconShape.SUNNY -> Morph(SunnyPolygon, CirclePolygon)
}

@Composable
fun rememberPressMorphShape(iconShape: IconShape, interactionSource: InteractionSource): Shape {
    val morph = remember(iconShape) { iconShape.pressMorph() } ?: return iconShape.toComposeShape()
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconPressMorph"
    )
    // Static shape at rest keeps identity stable so idle icons don't rebuild outlines.
    return if (progress == 0f) iconShape.toComposeShape() else MorphShape(morph, progress)
}
