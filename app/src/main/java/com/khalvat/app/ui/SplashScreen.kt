package com.khalvat.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.2f) }
    val logoRotation = remember { Animatable(-120f) }
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseValue"
    )

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(500)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
            )
        }
        launch { logoRotation.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) }
        delay(750)
        nameAlpha.animateTo(1f, tween(650))
        creditAlpha.animateTo(1f, tween(650))
        delay(950)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1B1F4B), Color(0xFF0B0D22)))
            )
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KhalvatLogo(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation.value
                        alpha = logoAlpha.value
                    },
                pulse = pulse
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "خلوت",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = nameAlpha.value
                    translationY = (1f - nameAlpha.value) * 40f
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "محافظ حریم خصوصی تماس و پیامک",
                color = Color(0xFFB7BAD6),
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer { alpha = nameAlpha.value }
            )
        }

        Text(
            "طراحی شده توسط Mr.PC",
            color = Brand.Cyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer {
                    alpha = creditAlpha.value
                    translationY = (1f - creditAlpha.value) * 24f
                }
        )
    }
}
