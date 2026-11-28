package com.khalvat.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Static Khalvat logo: gradient shield with a lock. */
@Composable
fun KhalvatLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val shield = Path().apply {
            moveTo(s * 0.5f, s * 0.06f)
            lineTo(s * 0.88f, s * 0.2f)
            lineTo(s * 0.88f, s * 0.52f)
            cubicTo(s * 0.88f, s * 0.76f, s * 0.72f, s * 0.9f, s * 0.5f, s * 0.97f)
            cubicTo(s * 0.28f, s * 0.9f, s * 0.12f, s * 0.76f, s * 0.12f, s * 0.52f)
            lineTo(s * 0.12f, s * 0.2f)
            close()
        }
        drawPath(
            shield,
            Brush.linearGradient(
                listOf(Brand.Indigo, Brand.Cyan),
                start = Offset(0f, 0f),
                end = Offset(s, s)
            )
        )
        val bodyW = s * 0.34f
        val bodyH = s * 0.26f
        val left = (s - bodyW) / 2f
        val top = s * 0.46f
        drawRoundRect(
            Color.White, Offset(left, top), Size(bodyW, bodyH), CornerRadius(s * 0.045f)
        )
        val sw = s * 0.05f
        val arcW = bodyW * 0.62f
        val arcLeft = (s - arcW) / 2f
        drawArc(
            Color.White, 180f, 180f, false,
            Offset(arcLeft, top - arcW / 2f), Size(arcW, arcW),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawLine(Color.White, Offset(arcLeft, top), Offset(arcLeft, top + sw), sw, StrokeCap.Round)
        drawLine(Color.White, Offset(arcLeft + arcW, top), Offset(arcLeft + arcW, top + sw), sw, StrokeCap.Round)
        drawCircle(Brand.Indigo, s * 0.03f, Offset(s / 2f, top + bodyH * 0.4f))
        drawLine(
            Brand.Indigo, Offset(s / 2f, top + bodyH * 0.45f),
            Offset(s / 2f, top + bodyH * 0.75f), s * 0.025f, StrokeCap.Round
        )
    }
}
