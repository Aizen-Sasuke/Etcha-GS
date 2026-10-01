package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val isCircle: Boolean,
    val rotationSpeed: Float
)

@Composable
fun ConfettiCelebration(
    isTriggered: Boolean,
    primaryAccent: Color = Color(0xFFFFD700),
    onFinished: () -> Unit = {}
) {
    if (!isTriggered) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            primaryAccent,
            Color(0xFFFFD700), // Gold
            Color(0xFFFF6B6B), // Coral
            Color(0xFF4ECDC4), // Turquoise
            Color(0xFF45B7D1), // Sky
            Color(0xFFA06CD5), // Purple
            Color(0xFFFFBE0B)  // Amber
        )
        List(50) {
            val angle = Random.nextDouble(Math.PI * 0.15, Math.PI * 0.85) // Upward spray
            val speed = Random.nextFloat() * 700f + 400f
            ConfettiParticle(
                initialX = 0.5f, // Centered horizontally
                initialY = 0.45f,
                vx = (cos(angle) * speed).toFloat() * (if (Random.nextBoolean()) 1f else -1f),
                vy = (-sin(angle) * speed).toFloat(),
                color = colors.random(),
                size = Random.nextFloat() * 10f + 8f,
                isCircle = Random.nextBoolean(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    LaunchedEffect(isTriggered) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas

        val w = size.width
        val h = size.height
        val gravity = 900f * p * p // accelerating gravity

        particles.forEach { pt ->
            val curX = (pt.initialX * w) + (pt.vx * p * 0.7f)
            val curY = (pt.initialY * h) + (pt.vy * p * 0.7f) + gravity
            val alpha = (1f - p).coerceIn(0f, 1f)
            val currentRotation = pt.rotationSpeed * p

            if (curX in 0f..w && curY in 0f..h) {
                rotate(degrees = currentRotation, pivot = Offset(curX, curY)) {
                    if (pt.isCircle) {
                        drawCircle(
                            color = pt.color.copy(alpha = alpha),
                            radius = pt.size / 2f,
                            center = Offset(curX, curY)
                        )
                    } else {
                        drawRect(
                            color = pt.color.copy(alpha = alpha),
                            topLeft = Offset(curX - pt.size / 2f, curY - pt.size / 4f),
                            size = Size(pt.size, pt.size * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
