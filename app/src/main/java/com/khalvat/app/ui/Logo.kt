package com.khalvat.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** لوگوی خلوت: قفل روی دایره گرادیانی با حلقه‌ی نبض‌دار. [pulse] بین ۰٫۸ تا ۱٫۲ تغییر می‌کند. */
@Composable
fun KhalvatLogo(modifier: Modifier = Modifier, pulse: Float = 1f) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(s / 2f, s / 2f)

        // حلقه‌های نبض
        drawCircle(
            color = Brand.Cyan.copy(alpha = 0.22f),
            radius = s * 0.48f * pulse,
            center = c,
            style = Stroke(width = s * 0.018f)
        )
        drawCircle(
            color = Brand.Indigo.copy(alpha = 0.16f),
            radius = s * 0.42f * (2f - pulse),
            center = c
        )

        // دایره اصلی
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Brand.Indigo, Brand.Cyan),
                start = Offset(0f, 0f),
                end = Offset(s, s)
            ),
            radius = s * 0.36f,
            center = c
        )

        // بدنه قفل
        val bodyW = s * 0.30f
        val bodyH = s * 0.23f
        val bodyLeft = c.x - bodyW / 2f
        val bodyTop = c.y - s * 0.02f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(s * 0.04f)
        )

        // کمان قفل
        val strokeW = s * 0.045f
        val arcW = bodyW * 0.62f
        val arcLeft = c.x - arcW / 2f
        val arcTop = bodyTop - s * 0.16f
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = Size(arcW, s * 0.16f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        val legTop = arcTop + s * 0.08f
        drawLine(Color.White, Offset(arcLeft, legTop), Offset(arcLeft, bodyTop + 2f), strokeW, StrokeCap.Round)
        drawLine(Color.White, Offset(arcLeft + arcW, legTop), Offset(arcLeft + arcW, bodyTop + 2f), strokeW, StrokeCap.Round)

        // سوراخ کلید
        drawCircle(Brand.Indigo, radius = s * 0.027f, center = Offset(c.x, bodyTop + bodyH * 0.42f))
        drawLine(
            Brand.Indigo,
            Offset(c.x, bodyTop + bodyH * 0.45f),
            Offset(c.x, bodyTop + bodyH * 0.75f),
            s * 0.02f,
            StrokeCap.Round
        )
    }
}
