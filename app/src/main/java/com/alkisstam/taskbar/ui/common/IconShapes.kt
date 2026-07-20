package com.alkisstam.taskbar.ui.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.alkisstam.taskbar.data.IconShape
import kotlin.math.abs
import kotlin.math.pow

private const val SQUIRCLE_EXPONENT = 4.0
private const val SQUIRCLE_SAMPLES = 48

// Compose has no built-in squircle; approximate a superellipse
// (|x|^n + |y|^n = 1) by sampling points around its boundary.
val SquircleShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    for (i in 0..SQUIRCLE_SAMPLES) {
        val t = 2 * Math.PI * i / SQUIRCLE_SAMPLES
        val cosT = Math.cos(t)
        val sinT = Math.sin(t)
        val x = cx + cx * sign(cosT) * abs(cosT).pow(2.0 / SQUIRCLE_EXPONENT)
        val y = cy + cy * sign(sinT) * abs(sinT).pow(2.0 / SQUIRCLE_EXPONENT)
        if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
    }
    close()
}

private fun sign(v: Double) = if (v < 0) -1.0 else 1.0

fun IconShape.toComposeShape(): Shape = when (this) {
    IconShape.DEFAULT, IconShape.SQUARE -> RectangleShape
    IconShape.SQUIRCLE -> SquircleShape
    IconShape.CIRCLE -> CircleShape
}
